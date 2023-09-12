package model.article

import model.article.repository.ArticlesRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val articlesModule = module {
    singleOf(::ArticlesRepository)
}