# Laporan Praktikum — News App Android

---

## 1. Identitas

| Field | Keterangan |
|---|---|
| **Nama** | Akmal Bobsaid |
| **NRP** | 5025231129 |
| **Mata Kuliah** | Pemrograman Perangkat Bergerak |
| **Pertemuan** | 14 |
| **GitHub** | https://github.com/akmalbobsaid/NewsApp |

---

## 2. Deskripsi Aplikasi

News App adalah aplikasi Android berbasis **Jetpack Compose** yang menampilkan berita terkini dari seluruh dunia menggunakan layanan **NewsAPI** (newsapi.org). Aplikasi ini memungkinkan pengguna untuk:

- Melihat **berita utama (top headlines)** berdasarkan kategori yang dipilih
- Menelusuri berita melalui fitur **pencarian real-time** dengan debounce 500ms
- Membaca **detail berita** lengkap beserta informasi penulis dan tanggal terbit
- **Membagikan** tautan berita ke aplikasi lain melalui Android Share Intent
- Membuka artikel lengkap di browser melalui tombol **"Read Full Article"**
- Melakukan **pull-to-refresh** untuk memperbarui daftar berita
- **Infinite scroll** — berita baru dimuat otomatis saat mendekati akhir daftar

Terdapat tujuh kategori berita yang tersedia: **General, Business, Entertainment, Health, Science, Sports,** dan **Technology**.

---

## 3. Teknologi yang Digunakan

### Bahasa & Platform
| Teknologi | Versi | Fungsi |
|---|---|---|
| Kotlin | 2.2.10 | Bahasa pemrograman utama |
| Android SDK | compileSdk 37, minSdk 24 | Platform target |
| Jetpack Compose | BOM 2026.02.01 | Framework UI deklaratif |
| Material3 | (via BOM) | Sistem desain UI |

### Jaringan & Data
| Library | Versi | Fungsi |
|---|---|---|
| Retrofit | 2.9.0 | HTTP client untuk konsumsi REST API |
| Gson Converter | 2.9.0 | Konversi JSON ke data class Kotlin |
| OkHttp | 4.12.0 | Layer HTTP dasar dengan connection timeout |
| OkHttp Logging Interceptor | 4.12.0 | Logging request/response pada debug build |

### UI & Navigasi
| Library | Versi | Fungsi |
|---|---|---|
| Coil Compose | 2.5.0 | Pemuatan dan caching gambar asinkron |
| Navigation Compose | 2.9.0 | Navigasi antar layar dengan NavHost |
| Material Icons Core | (via BOM) | Ikon Material Design (Search, Share, ArrowBack) |

### Arsitektur & Concurrency
| Library | Versi | Fungsi |
|---|---|---|
| Lifecycle ViewModel Compose | 2.11.0 | ViewModel scope untuk Compose |
| Lifecycle Runtime Compose | 2.11.0 | `collectAsStateWithLifecycle` |
| Kotlin Coroutines Android | 1.9.0 | Pemrograman asinkron non-blocking |

### Konfigurasi
- **BuildConfig** — API key dan BASE_URL disimpan aman di `local.properties`, diekspos ke kode via `buildConfigField`
- **AGP** 9.2.1 — Android Gradle Plugin untuk proses build

---

## 4. Penjelasan Kode Utama

### 4.1 Struktur Paket

```
com.example.newsapp/
├── data/
│   ├── api/
│   │   ├── ApiService.kt          # Retrofit interface
│   │   └── RetrofitInstance.kt    # Singleton Retrofit + OkHttp
│   ├── model/
│   │   └── News.kt                # Data class: NewsResponse, Article, Source
│   └── repository/
│       └── NewsRepository.kt      # Repository + sealed class Result<T>
├── viewmodel/
│   └── NewsViewModel.kt           # ViewModel + NewsUiState
├── ui/
│   ├── component/
│   │   └── NewsCard.kt            # NewsCard, NewsCardFeatured, NewsCardSkeleton
│   ├── screen/
│   │   ├── HomeScreen.kt
│   │   ├── SearchScreen.kt
│   │   └── DetailScreen.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── MainActivity.kt                # Routes, NewsApp(), entry point
```

---

### 4.2 Data Layer

#### `News.kt` — Model Data
```kotlin
data class Article(
    @SerializedName("title")       val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("url")         val url: String?,
    @SerializedName("urlToImage")  val urlToImage: String?,
    @SerializedName("publishedAt") val publishedAt: String?,
    @SerializedName("source")      val source: Source?,
    @SerializedName("author")      val author: String?,
    @SerializedName("content")     val content: String?
) {
    val safeTitle: String   get() = title?.takeIf { it != "[Removed]" } ?: ""
    val safeContent: String get() = content?.takeIf { it != "[Removed]" } ?: ""
}
```
Seluruh field bersifat **nullable** karena NewsAPI tidak menjamin ketersediaan setiap field. Computed property `safeTitle` dan `safeContent` memfilter nilai `null` dan string `"[Removed]"` yang dikembalikan API untuk artikel yang dihapus.

#### `ApiService.kt` — Retrofit Interface
```kotlin
interface ApiService {
    @GET("v2/top-headlines")
    suspend fun getTopHeadlines(
        @Query("apiKey")   apiKey: String,
        @Query("country")  country: String,
        @Query("category") category: String?,
        @Query("pageSize") pageSize: Int,
        @Query("page")     page: Int
    ): NewsResponse

    @GET("v2/everything")
    suspend fun searchNews(
        @Query("apiKey")  apiKey: String,
        @Query("q")       q: String,
        @Query("sortBy")  sortBy: String,
        @Query("pageSize") pageSize: Int,
        @Query("page")    page: Int
    ): NewsResponse
}
```
Semua parameter dideklarasikan **tanpa nilai default** untuk menjamin kompatibilitas dengan mekanisme Java `Proxy` yang digunakan Retrofit. Nilai default ("us", "publishedAt", 20) ditangani di sisi repository.

#### `RetrofitInstance.kt` — Singleton Network Client
```kotlin
object RetrofitInstance {
    private val okHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
```
Logging HTTP hanya aktif pada **debug build** untuk menghindari kebocoran informasi di release. Timeout 30 detik ditetapkan pada ketiga fase koneksi.

#### `NewsRepository.kt` — Repository dengan Sealed Result
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

class NewsRepository {
    suspend fun getTopHeadlines(category: String?, page: Int): Result<List<Article>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getTopHeadlines(
                    apiKey = BuildConfig.NEWS_API_KEY, country = "us",
                    category = category, pageSize = 20, page = page
                )
                val articles = response.articles
                    ?.filter { it.title != null && it.title != "[Removed]" }
                    ?: emptyList()
                Result.Success(articles)
            } catch (e: Exception) {
                Result.Error(e.message ?: "An unknown error occurred")
            }
        }
}
```
Pola `sealed class Result<T>` memisahkan state sukses dan error secara type-safe. Semua operasi jaringan dijalankan di `Dispatchers.IO` menggunakan `withContext` agar tidak memblokir Main thread.

---

### 4.3 ViewModel

#### `NewsViewModel.kt` — State Management
```kotlin
data class NewsUiState(
    val headlines: List<Article>      = emptyList(),
    val searchResults: List<Article>  = emptyList(),
    val isLoadingHeadlines: Boolean   = false,
    val isLoadingSearch: Boolean      = false,
    val isLoadingMore: Boolean        = false,
    val headlinesError: String?       = null,
    val searchError: String?          = null,
    val selectedCategory: String      = "general",
    val searchQuery: String           = "",
    val currentPage: Int              = 1,
    val hasMoreHeadlines: Boolean     = true
)
```

Seluruh state UI dikemas dalam satu `data class` yang diekspos sebagai `StateFlow` — immutable dari luar ViewModel. Perubahan state dilakukan secara atomic dengan `_uiState.update { }`.

**Fitur utama ViewModel:**

- **Debounce pencarian** — `searchJob` dibatalkan lalu dibuat ulang dengan `delay(500)` setiap kali query berubah, mencegah permintaan API berlebihan
- **Pagination** — `loadTopHeadlines(loadMore = true)` menambahkan halaman baru ke daftar yang ada; `hasMoreHeadlines` di-update berdasarkan jumlah artikel yang dikembalikan (< 20 berarti sudah habis)
- **Job cancellation** — `headlinesJob?.cancel()` sebelum request baru mencegah race condition antar-panggilan
- **Category switching** — `selectCategory()` mereset halaman dan mengosongkan daftar sebelum memuat ulang

---

### 4.4 Komponen UI

#### `NewsCard.kt`

Terdapat tiga varian komponen kartu berita:

| Komponen | Deskripsi |
|---|---|
| `NewsCard` | Kartu vertikal `fillMaxWidth` dengan gambar 200dp, chip sumber, judul (2 baris), dan deskripsi |
| `NewsCardFeatured` | Kartu horizontal 280×200dp dengan `AsyncImage` penuh, gradient scrim hitam di bawah, dan teks overlay |
| `NewsCardSkeleton` | Placeholder shimmer dengan animasi alpha 0.3f↔1f menggunakan `rememberInfiniteTransition` |

```kotlin
// Gradient scrim pada NewsCardFeatured
Box(
    modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
        )
    )
)
```

---

### 4.5 Layar Utama

#### `HomeScreen.kt`
- **`PullToRefreshBox`** (Material3 Experimental) membungkus `LazyColumn`
- **Infinite scroll** diimplementasikan dengan `derivedStateOf` yang mendeteksi apakah item terakhir yang terlihat sudah ≤ 3 item dari akhir daftar
- Struktur `LazyColumn` menggunakan `when` di dalam scope-nya untuk beralih antar tiga state: skeleton (loading awal), error, dan konten normal
- Tiga artikel pertama ditampilkan di `LazyRow` horizontal sebagai **Featured**, sisanya di bagian **Latest News**

```kotlin
val shouldLoadMore by remember {
    derivedStateOf {
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            ?: return@derivedStateOf false
        val totalItems = listState.layoutInfo.totalItemsCount
        totalItems > 0 && lastVisible >= totalItems - 3
    }
}
```

#### `SearchScreen.kt`
- `OutlinedTextField` di dalam `TopAppBar` dengan **auto-focus** via `FocusRequester` + `LaunchedEffect(Unit)`
- Empat state: query kosong (ilustrasi), loading (3 skeleton), hasil kosong (pesan), dan daftar hasil dengan infinite scroll
- Infinite scroll pada hasil pencarian memanggil `viewModel.loadMoreSearchResults()`

#### `DetailScreen.kt`
- **Hero image** 240dp dengan `ContentScale.Crop`
- Avatar lingkaran berisi inisial pertama nama penulis
- Tanggal diformat dari ISO 8601 (`yyyy-MM-dd'T'HH:mm:ss'Z'`) ke `dd MMM yyyy, HH:mm` menggunakan `SimpleDateFormat`
- **Share** via `Intent.ACTION_SEND` dan **buka browser** via `Intent.ACTION_VIEW`

---

### 4.6 Navigasi

```kotlin
object Routes {
    const val HOME   = "home"
    const val SEARCH = "search"
    const val DETAIL = "detail"
}
```

`NavHost` dengan tiga destinasi dikelola di `NewsApp()`. `selectedArticle` disimpan sebagai `remember { mutableStateOf<Article?>(null) }` di level NavHost agar dapat diakses oleh layar detail tanpa perlu serialisasi. Guard `return@composable` pada rute DETAIL mencegah crash jika state null.

---

## 5. Langkah Pengerjaan

1. **Setup proyek** — Buat proyek Android Studio baru (Empty Compose Activity), tambahkan API key NewsAPI ke `local.properties`, ekspos via `BuildConfig`
2. **Konfigurasi Gradle** — Tambahkan dependensi Retrofit, OkHttp, Coil, Navigation Compose, Lifecycle, Coroutines, dan Material Icons Core ke `libs.versions.toml` dan `app/build.gradle.kts`; aktifkan `buildConfig = true`
3. **Manifest** — Tambahkan izin `INTERNET` dan set `usesCleartextTraffic="false"`
4. **Data layer** — Buat model (`News.kt`), interface API (`ApiService.kt`), singleton network client (`RetrofitInstance.kt`), dan repository dengan sealed `Result<T>` (`NewsRepository.kt`)
5. **ViewModel** — Implementasikan `NewsUiState`, `NewsViewModel` dengan StateFlow, logika pagination, debounce pencarian, dan category switching
6. **Komponen UI** — Buat tiga varian `NewsCard` termasuk shimmer skeleton
7. **Layar** — Implementasikan `HomeScreen` (pull-to-refresh, infinite scroll, featured section), `SearchScreen` (auto-focus, 4 state), dan `DetailScreen` (share, buka URL, format tanggal)
8. **Navigasi** — Hubungkan semua layar di `MainActivity.kt` menggunakan `NavHost` dan `Routes`
9. **Perbaikan build** — Perbaiki missing dependency `material-icons-core`, hapus Kotlin default params dari Retrofit interface, update `compileSdk` ke 37

---

## 6. Hasil Aplikasi

### Home Screen
Layar utama menampilkan daftar berita yang dapat di-refresh dengan pull-to-refresh. Di bagian atas terdapat chip filter kategori (General, Business, dll). Tiga berita teratas ditampilkan sebagai kartu horizontal bergambar penuh di bagian **Featured**. Di bawahnya, berita lainnya ditampilkan sebagai daftar vertikal di bagian **Latest News**. Saat data sedang dimuat pertama kali, ditampilkan animasi shimmer skeleton sebagai placeholder.

### Search Screen
Layar pencarian terbuka dengan keyboard muncul otomatis. Pengguna mengetik query dan hasil muncul setelah jeda 500ms. Saat query kosong ditampilkan ikon pencarian dan teks panduan. Saat tidak ada hasil ditemukan ditampilkan pesan informatif. Hasil pencarian mendukung scroll tak terbatas untuk memuat lebih banyak artikel.

### Detail Screen
Layar detail menampilkan gambar artikel berukuran besar di bagian atas, diikuti nama sumber, judul lengkap, avatar dan nama penulis, tanggal terbit, deskripsi (cetak tebal), dan isi artikel. Di bagian bawah terdapat tombol **"Read Full Article"** untuk membuka artikel lengkap di browser, serta tombol share di toolbar untuk berbagi tautan.

### Fitur Lain
- Infinite scroll aktif di Home dan Search — berita baru dimuat otomatis saat mendekati akhir daftar
- Error state dengan tombol **Retry** jika koneksi gagal
- Navigasi back yang konsisten di semua layar
- Desain mengikuti **Material Design 3** dengan dukungan dynamic color pada Android 12+
