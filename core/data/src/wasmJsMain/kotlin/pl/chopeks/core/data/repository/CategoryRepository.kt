package pl.chopeks.core.data.repository

import pl.chopeks.core.data.Backend
import pl.chopeks.core.data.utils.RpcWrapper
import pl.chopeks.core.model.Category
import pl.chopeks.core.model.Video

class CategoryRepository(
	private val delegate: ICategoryRepository,
) : ICategoryRepository, RpcWrapper {
	override suspend fun getCategories(): List<Category> = rpc {
		delegate.getCategories().map {
			it.copy(image = "${Backend.URL}api/image/${it.id}/category")
		}
	}

	override suspend fun getImage(category: Category): String? = null

	override suspend fun getImageBytes(category: Category): ByteArray? = rpc {
		delegate.getImageBytes(category)
	}

	override suspend fun bind(category: Category, video: Video) = rpc {
		delegate.bind(category, video)
	}

	override suspend fun unbind(category: Category, video: Video) = rpc {
		delegate.unbind(category, video)
	}

	override suspend fun add(name: String, image: String?) = rpc {
		delegate.add(name, image)
	}

	override suspend fun edit(id: Int, name: String, image: String?) = rpc {
		delegate.edit(id, name, image)
	}

	override suspend fun delete(category: Category) = rpc {
		delegate.delete(category)
	}
}
