package pl.chopeks.core.data.repository

import pl.chopeks.core.data.utils.RpcWrapper
import pl.chopeks.core.model.Category
import pl.chopeks.core.model.Video

class CategoryRepository(
	private val delegate: ICategoryRepository,
) : ICategoryRepository, RpcWrapper {
	override suspend fun getCategories(): List<Category> = rpc {
		delegate.getCategories()
	}

	override suspend fun getImage(category: Category): String? = rpc {
		delegate.getImage(category)
	}

	override suspend fun bind(category: Category, video: Video) = rpc {
		delegate.bind(category, video)
	}

	override suspend fun unbind(category: Category, video: Video) = rpc {
		delegate.unbind(category, video)
	}

	override suspend fun add(name: String, url: String?) = rpc {
		delegate.add(name, url)
	}

	override suspend fun edit(id: Int, name: String, url: String?) = rpc {
		delegate.edit(id, name, url)
	}

	override suspend fun delete(category: Category) = rpc {
		delegate.delete(category)
	}
}