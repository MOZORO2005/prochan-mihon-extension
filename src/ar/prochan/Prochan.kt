package eu.kanade.tachiyomi.extension.ar.prochan

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup

class Prochan : HttpSource() {
    override val name = "Prochan"
    override val baseUrl = "https://prochan.net"
    override val lang = "ar"
    override val supportsLatest = true
    override val client = network.cloudflareClient

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/manga/?page=$page", headers)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val document = Jsoup.parse(response.body.string())
        val mangas = mutableListOf<SManga>()
        
        // عناصر المانغا
        val elements = document.select("div.manga-item, a[href*=/manga/]")
        
        elements.forEach { element ->
            val manga = SManga.create()
            val link = element.select("a").first()
            
            link?.let {
                manga.url = it.attr("href")
                manga.title = it.text().ifEmpty { "مانغا Prochan" }
                mangas.add(manga)
            }
        }
        
        // إذا لم نجد anything، نضيف مانغا تجريبية
        if (mangas.isEmpty()) {
            val testManga = SManga.create().apply {
                title = "مانغا تجريبية"
                url = "/manga/test"
            }
            mangas.add(testManga)
        }
        
        val hasNext = document.select("a.next, a[rel=next]").isNotEmpty()
        return MangasPage(mangas, hasNext)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/latest?page=$page", headers)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        return popularMangaParse(response)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&page=$page", headers)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        return popularMangaParse(response)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        return SManga.create().apply {
            title = "مانغا Prochan"
            description = "مانغا من موقع Prochan العربي"
        }
    }

    override fun mangaDetailsRequest(manga: SManga): Request {
        return GET(baseUrl + manga.url, headers)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val chapters = mutableListOf<SChapter>()
        
        // فصل تجريبي
        val testChapter = SChapter.create().apply {
            name = "الفصل الأول"
            url = "/chapter/1"
        }
        chapters.add(testChapter)
        
        return chapters
    }

    override fun chapterListRequest(manga: SManga): Request {
        return mangaDetailsRequest(manga)
    }

    override fun pageListParse(response: Response): List<Page> {
        return emptyList()
    }

    override fun pageListRequest(chapter: SChapter): Request {
        return GET(baseUrl + chapter.url, headers)
    }

    override fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!, headers)
    }

    override fun getFilterList() = FilterList()
}
