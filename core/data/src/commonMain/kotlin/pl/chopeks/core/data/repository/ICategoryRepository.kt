package pl.chopeks.core.data.repository

import pl.chopeks.core.model.Category
import pl.chopeks.core.model.Video

interface ICategoryRepository : AutoCloseable {
	suspend fun getCategories(): List<Category>
	suspend fun getImage(category: Category): String?
	suspend fun bind(category: Category, video: Video)
	suspend fun unbind(category: Category, video: Video)
	suspend fun add(name: String, url: String)
	suspend fun edit(id: Int, name: String, url: String)
}