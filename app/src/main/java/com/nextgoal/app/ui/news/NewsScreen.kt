package com.nextgoal.app.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nextgoal.app.data.model.NewsArticleUi
import com.nextgoal.app.ui.common.ViewModelFactories
import com.nextgoal.app.ui.components.NextGoalTopBar
import com.nextgoal.app.ui.components.InfoBanner
import com.nextgoal.app.ui.components.SectionHeader
import com.nextgoal.app.ui.theme.BgCard
import com.nextgoal.app.ui.theme.BgCardAlt
import com.nextgoal.app.ui.theme.BorderSubtle
import com.nextgoal.app.ui.theme.NextGoalBlue
import com.nextgoal.app.ui.theme.TextPrimary
import com.nextgoal.app.ui.theme.TextSecondary
import com.nextgoal.app.ui.theme.TextTertiary
import coil.compose.AsyncImage

@Composable
fun NewsScreen(onSearchClick: () -> Unit, viewModel: NewsViewModel = viewModel(factory = ViewModelFactories.News)) {
    val featured = viewModel.articles.firstOrNull { it.isFeatured }
    val rest = viewModel.articles.filterNot { it.isFeatured }

    Column(modifier = Modifier.fillMaxSize().background(com.nextgoal.app.ui.theme.BgDeep)) {
        NextGoalTopBar(onSearchClick = onSearchClick)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { SectionHeader("NOTICIAS") }
            featured?.let { article ->
                item {
                    FeaturedNewsCard(article)
                    Spacer(Modifier.height(12.dp))
                    SectionHeader("MÁS NOTICIAS")
                }
            }
            if (viewModel.articles.isEmpty()) item {
                InfoBanner("No hay noticias publicadas por la fuente actual.", Modifier.padding(16.dp))
            }
            items(rest) { article -> NewsRow(article) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun FeaturedNewsCard(article: NewsArticleUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(BgCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
            .padding(bottom = 14.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp).background(BgCardAlt),
            contentAlignment = Alignment.Center
        ) {
            if (!article.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = article.title,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Article, contentDescription = null, tint = NextGoalBlue, modifier = Modifier.size(30.dp))
                    Text("Sin imagen disponible", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(article.category, color = NextGoalBlue, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                Text("  ·  ${article.timeAgo}", color = TextTertiary, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(9.dp))
            Text(article.title, color = TextPrimary, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(7.dp))
            Text(article.summary, color = TextSecondary, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, maxLines = 3)
        }
    }
}

@Composable
private fun NewsRow(article: NewsArticleUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(BgCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(BgCardAlt), contentAlignment = Alignment.Center) {
            if (!article.imageUrl.isNullOrBlank()) {
                AsyncImage(model = article.imageUrl, contentDescription = article.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Outlined.Article, contentDescription = null, tint = NextGoalBlue, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(article.category, color = NextGoalBlue, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            Text(article.title, color = TextPrimary, style = androidx.compose.material3.MaterialTheme.typography.titleSmall, maxLines = 2)
            Text(article.timeAgo, color = TextTertiary, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
        }
    }
}
