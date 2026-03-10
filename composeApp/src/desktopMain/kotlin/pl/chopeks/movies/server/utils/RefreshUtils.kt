package pl.chopeks.movies.server.utils

import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.core.database.AudioToBeCheckedTable
import pl.chopeks.core.database.DetectedDuplicatesTable
import pl.chopeks.core.database.MovieTable
import pl.chopeks.core.database.MoviesToBeCheckedTable
import pl.chopeks.core.database.PathsTable
import pl.chopeks.core.utils.getDirectories
import pl.chopeks.core.utils.getFiles
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import kotlin.system.measureTimeMillis

object RefreshUtils {
	@OptIn(DelicateCoroutinesApi::class)
	fun refresh(onEvent: (String) -> Unit) {
		transaction {
			PathsTable.selectAll().map { Pair(File(it[PathsTable.path]), it[PathsTable.count]) }
		}.forEach {
			onEvent("Checking ${it.first.absolutePath}.")
			val fileCount = getFiles(it.first).size.toLong()

			val shouldCheckFiles = transaction {
				val dbCount = MovieTable.selectAll().where { MovieTable.path like "${it.first.absolutePath}%" }.count()
				if (dbCount != fileCount) {
					onEvent("File count different, checking which files were removed or added.")
				}
				dbCount != fileCount
			}

			if (shouldCheckFiles) {
				runBlocking {
					newFixedThreadPoolContext(32, "file-check").use { pool ->
						getDirectories(it.first).map { dir ->
							async(pool) {
								transaction {
									val dbPaths = MovieTable.select(MovieTable.path)
										.where { MovieTable.path like "${dir.absolutePath}%" }
										.map { it[MovieTable.path] }
									getFiles(dir).filter { it.absolutePath !in dbPaths }
								}
							}
						}.awaitAll().flatten().sortedBy { it.absolutePath }.also { files ->
							transaction {
								files.forEach {
									onEvent("Added ${it.absolutePath}.")
									val movie = MovieTable.insert { new ->
										new[MovieTable.name] = it.nameWithoutExtension
										new[MovieTable.path] = it.absolutePath
									}
									MoviesToBeCheckedTable.insert { it[MoviesToBeCheckedTable.id] = movie[MovieTable.id] }
									AudioToBeCheckedTable.insert { it[AudioToBeCheckedTable.id] = movie[MovieTable.id] }
								}
							}
						}
					}
				}
			}

			transaction {
				MovieTable.selectAll().where { MovieTable.thumbnail.isNull() }.map { Pair(it[MovieTable.id].value, it[MovieTable.path]) }
			}.forEach { movie ->
				try {
					measureTimeMillis {
						makeScreenshot(File(movie.second)).also { img ->
							val bytes = ImageIO.read(img.inputStream())
								.normalizeImage()
								.let {
									ByteArrayOutputStream().use { os ->
										ImageIO.write(it, "jpg", os)
										os.toByteArray()
									}
								}
							transaction {
								MovieTable.update({ MovieTable.id eq movie.first }, body = {
									it[MovieTable.thumbnail] = "data:image/jpg;base64," + String(Base64.getMimeEncoder().encode(bytes))
								})
							}
						}
					}.let { time -> println("${time}ms") }
					onEvent("Added thumbnail of ${movie.second}.")
				} catch (e: Throwable) {
					e.printStackTrace()
				}
			}

			transaction {
				mutableListOf<Pair<Int, String>>().apply {
					MovieTable.selectAll().where { MovieTable.duration.isNull() or MovieTable.duration.eq(0) }.forEach { add(Pair(it[MovieTable.id].value, it[MovieTable.path])) }
				}
			}.forEach { movie ->
				getVideoDuration(File(movie.second)).also { dur ->
					transaction {
						MovieTable.update({ MovieTable.id eq movie.first }, body = {
							it[MovieTable.duration] = dur.toInt()
						})
					}
				}
				onEvent("Added duration of ${movie.second}.")
			}
			onEvent("Directory check completed.")
		}

//    markAllMoviesToBeChecked()
	}

	fun markAllMoviesToBeChecked() {
		transaction {
//			DetectedDuplicatesTable.deleteAll()

//			MoviesToBeCheckedTable.deleteAll()
//			MovieTable.select(MovieTable.id).orderBy(MovieTable.id, SortOrder.DESC).take(5).map { it[MovieTable.id] }.forEach { movieId ->
//				MoviesToBeCheckedTable.insert { it[MoviesToBeCheckedTable.id] = movieId }
//			}

			AudioToBeCheckedTable.deleteAll()
			MovieTable.select(MovieTable.id).orderBy(MovieTable.id, SortOrder.DESC).map { it[MovieTable.id] }.forEach { movieId ->
				AudioToBeCheckedTable.insert { it[AudioToBeCheckedTable.id] = movieId }
			}
		}
	}

}