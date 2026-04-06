package pl.chopeks.core.data.repository

import pl.chopeks.core.database.datasource.CategoriesDataSource
import pl.chopeks.core.model.Category
import pl.chopeks.core.model.Video
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class CategoryRepository(
	private val dataSource: CategoriesDataSource,
) : ICategoryRepository {
	override suspend fun getCategories(): List<Category> {
		return dataSource.getCategories()
	}

	override suspend fun getImage(category: Category): String? {
		return dataSource.getImage(category)
	}

	@OptIn(ExperimentalEncodingApi::class)
	override suspend fun getImageBytes(category: Category): ByteArray? {
		return getImage(category)?.let { Base64.Mime.decode(it) }
	}

	override suspend fun bind(category: Category, video: Video) {
		dataSource.bind(category, video)
	}

	override suspend fun unbind(category: Category, video: Video) {
		dataSource.unbind(category, video)
	}

	override suspend fun add(name: String, image: String?) {
		dataSource.add(name, image)
	}

	override suspend fun edit(id: Int, name: String, image: String?) {
		dataSource.edit(id, name, image)
	}

	override suspend fun delete(category: Category) {
		dataSource.delete(category)
	}
}
