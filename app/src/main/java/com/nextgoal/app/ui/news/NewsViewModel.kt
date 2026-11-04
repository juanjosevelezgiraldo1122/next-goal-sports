package com.nextgoal.app.ui.news

import androidx.lifecycle.ViewModel
import com.nextgoal.app.data.model.NewsArticleUi
import com.nextgoal.app.data.repository.NewsRepository

class NewsViewModel(repository: NewsRepository) : ViewModel() {
    val articles: List<NewsArticleUi> = repository.getSampleArticles()
}
