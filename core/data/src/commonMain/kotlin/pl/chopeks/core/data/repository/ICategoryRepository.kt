package pl.chopeks.core.data.repository

import kotlinx.rpc.annotations.Rpc
import pl.chopeks.core.model.Category
import pl.chopeks.core.model.Video

@Rpc
interface ICategoryRepository {
	suspend fun getCategories(): List<Category>
	suspend fun getImage(category: Category): String?
	suspend fun bind(category: Category, video: Video)
	suspend fun unbind(category: Category, video: Video)
	suspend fun add(name: String, image: String?)
	suspend fun edit(id: Int, name: String, image: String?)
	suspend fun delete(category: Category)
}
