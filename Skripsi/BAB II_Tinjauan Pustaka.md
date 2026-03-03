# BAB II TINJAUAN PUSTAKA

Bab II merupakan tinjauan Pustaka dari laporan tugas akhir. Bab II
membahas state of the art, definisi, dan teori-teori yang digunakan
dalam penelitian. Kumpulan teori tersebut digunakan sebagai penunjang
dalam pembuatan penelitian tugas akhir.

## State of The Art

Penelitian mengenai sistem rekam medis elektronik dan manajemen pasien
telah banyak dilakukan dengan berbagai metode dan platform. Berikut
adalah ulasan terhadap penelitian terdahulu yang menjadi acuan dalam
pengembangan sistem manajemen pasien PhysioNet berbasis Android:

Penelitian sebelumnya yang dilakukan oleh (Saputra, Syaputra and
Harmanto, 2024) merancang aplikasi pelayanan rekam medis elektronik
berbasis *smartphone* Android menggunakan metode *Waterfall*. Sistem ini
difokuskan untuk memenuhi regulasi pemerintah mengenai kewajiban
implementasi RME di rumah sakit. Hasil penelitian menunjukkan bahwa
penggunaan aplikasi *mobile* dapat mengurangi risiko kehilangan data dan
*human error* pada proses administrasi manual di Rumah Sakit Rafflesia.
Namun, penelitian ini masih mencakup layanan kesehatan secara umum di
tingkat rumah sakit, belum menyentuh kebutuhan spesifik layanan
rehabilitasi fisik.

Penelitian lain oleh (Firdonsyah and Dewi, 2024) membangun sistem rekam
medis elektronik berbasis *web* yang diimplementasikan pada praktik
mandiri fisioterapi Biophisyo Yogyakarta. Penelitian ini memberikan
gambaran mendalam mengenai variabel data dan alur kerja khusus
fisioterapi, mulai dari tahap asesmen, diagnosa, hingga intervensi.
Keunggulan sistem ini adalah kemampuannya mendokumentasikan proses
klinis secara mendetail, namun keterbatasannya terletak pada penggunaan
platform berbasis *web* yang memerlukan konektivitas internet konstan
dan kurang fleksibel untuk mobilitas terapis di lapangan.

Selanjutnya, (Karsana and Kurniawijaya, 2022) mengembangkan sistem
informasi manajemen pasien fisioterapi pada Klinik Maha Bhoga Marga
menggunakan model SDLC *Waterfall*. Sistem ini mencakup fitur
pendaftaran, riwayat berobat, dan pencatatan tindakan pasien untuk
mempercepat pencarian data historis. Penelitian ini memperkuat urgensi
transisi ke sistem digital guna menghindari keterlambatan evaluasi
perkembangan terapi yang sering terjadi pada sistem manual, meskipun
implementasinya masih terbatas pada perangkat komputer meja (*desktop*).

Penelitian yang dilakukan oleh (Khabib, Rozi and Rosadi, 2023)
membuktikan efektivitas penggunaan perangkat *mobile* Android untuk
manajemen rekam medis di Puskesmas Bulukandang. Dengan hasil pengujian
*System Usability Scale* (SUS) mencapai skor 81 (kategori *Acceptable*),
penelitian ini menunjukkan bahwa tenaga kesehatan lebih mudah menerima
sistem digital yang memiliki fleksibilitas tinggi. Fokus penelitian ini
adalah pada pemilihan platform Android Studio sebagai *environment*
pengembangan, yang memberikan performa aplikasi lebih stabil
dibandingkan solusi berbasis *web*.

Penelitian sebelumnya mengimplementasikan aplikasi manajemen klinik
pratama pada Klinik Laa Tachzan untuk mengatasi kendala pengelolaan data
manual berbasis kertas. Sistem ini mengintegrasikan fitur manajemen
antrean pasien, pencatatan rekam medis, hingga pengelolaan nota
pembayaran. Fokus utama penelitian ini adalah meningkatkan efisiensi
operasional dan menyediakan informasi jadwal dokter secara *real-time*.
Meskipun memberikan solusi manajemen klinik yang komprehensif, sistem
ini masih diimplementasikan pada lingkup layanan kesehatan umum dan
belum dioptimalkan untuk kebutuhan spesifik pendokumentasian terapi
fisik yang berulang (Mulyani *et al.*, 2021)

Penelitian yang dilakukan oleh (Tyas *et al.*, 2024) yang berjudul
pengembangan dan implementasi Sistem Rekam Medis Elektronik (EMR) untuk
klinik fisioterapi di Yogyakarta, Indonesia. Artikel ini membahas proses
digitalisasi rekam medis manual untuk meningkatkan pengelolaan data,
aksesibilitas, dan pengambilan keputusan di klinik tersebut. Metode yang
digunakan meliputi analisis rekam medis manual, perancangan basis data
relasional menggunakan ERD dan DOD, serta pengembangan sistem pilot
berbasis kerangka kerja CI dan desain antarmuka menggunakan Figma. Hasil
dari penelitian ini adalah terciptanya sistem EMR pilot yang terstruktur
dan dapat dikembangkan untuk mendukung proses pembayaran dan kerjasama
di masa depan, yang diharapkan dapat meningkatkan kualitas dan efisiensi
layanan fisioterapi sesuai standar layanan kesehatan dan kemajuan
teknologi Industry 4.0.

Penelitian yang dilakukan oleh (Omran, 2023) dengan judul pengukuran
waktu pengambilan data dari Firebase Cloud untuk aplikasi E-Health
berbasis Android, serta dampaknya terhadap pengiriman berbagai jenis
gambar medis seperti X-ray, CT, PET, ultrasound, dan JPEG. Jurnal ini
membahas pengembangan sistem kesehatan berbasis cloud yang memanfaatkan
Firebase untuk penyimpanan data medis, termasuk fitur pencarian layanan,
pemesanan janji, akses rekam medis, dan komunikasi antara pasien dan
profesional kesehatan, terutama dalam situasi darurat. Metode yang
digunakan meliputi pengujian waktu pengambilan data dari Firebase,
pengimplementasian aplikasi Android yang terintegrasi dengan Firebase,
serta penerapan algoritma keamanan seperti AES untuk melindungi data
sensitif. Hasil penelitian menunjukkan bahwa penggunaan Firebase
memungkinkan pengambilan data yang cepat dan efisien, mendukung
pengiriman gambar medis berukuran besar, serta meningkatkan
responsivitas dan keamanan layanan kesehatan berbasis mobile, sehingga
dapat mempercepat proses diagnosis dan penanganan darurat di lingkungan
kesehatan.

Penelitian yang dilakukan oleh (Ghazalba and Anggara, 2024) dengan judul
pengembangan sistem pendaftaran online dan rekam medis berbasis *web*
dan Android untuk klinik. Jurnal ini membahas tentang pembuatan sistem
yang bertujuan meningkatkan efisiensi layanan kesehatan, mengurangi
antrean dan kesalahan administrasi, serta memudahkan akses data medis
pasien melalui teknologi REST API, Laravel, dan Kotlin. Metode yang
digunakan dalam pengembangan sistem ini adalah metode Waterfall, yang
meliputi tahapan analisis kebutuhan, perancangan, implementasi,
pengujian, dan pemeliharaan. Pengujian sistem dilakukan dengan metode
black box testing untuk memastikan semua fitur seperti login,
pendaftaran, upload rekam medis, dan pengelolaan data berjalan sesuai
harapan. Hasil dari penelitian menunjukkan bahwa sistem berhasil
memenuhi kebutuhan pengguna, meningkatkan efisiensi layanan klinik,
mempercepat proses administrasi, dan mendukung digitalisasi layanan
kesehatan, dengan saran peningkatan keamanan data, performa, serta
penambahan fitur notifikasi dan akses platform iOS untuk pengalaman
pengguna yang lebih baik.

Penelitian dilakukan oleh (Andriani, Wulandari and Margianti, 2022)
dengan judul implementasi dan manfaat Rekam Medis Elektronik (RME) di
Rumah Sakit Universitas Gadjah Mada. Jurnal ini membahas berbagai aspek
terkait penggunaan RME, termasuk persepsi pengguna seperti dokter,
perawat, apoteker, dan petugas rekam medis terhadap sistem tersebut,
serta manfaatnya dalam meningkatkan keselamatan pasien, efisiensi
pelayanan, dan kolaborasi antar tenaga kesehatan. Metode yang digunakan
adalah penelitian kualitatif dengan studi kasus, yang melibatkan
observasi dan wawancara untuk mengumpulkan data tentang fitur dan
manfaat RME. Hasil penelitian menunjukkan bahwa RME mendukung deteksi
alergi, kontraindikasi obat, mengurangi duplikasi pemeriksaan,
mempercepat pengambilan keputusan klinis, dan memfasilitasi komunikasi
antar tenaga kesehatan. Pengguna merasakan manfaat besar dari
fitur-fitur RME, meskipun pelatihan internal masih diperlukan untuk staf
baru, dan pengembangan fitur seperti pengingat otomatis disarankan untuk
meningkatkan manfaat sistem secara berkelanjutan.

Penelitian dilakukan oleh (Pratama *et al.*, 2023) dengan judul
\"Pengembangan dan Implementasi Sistem Rekam Medis Elektronik (EMR) di
Poliklinik Polije Menggunakan Model SaaS dan Nomor Identitas Tunggal
(SIN)\". Artikel ini membahas proses pengembangan dan penerapan sistem
EMR berbasis SaaS yang menggunakan nomor identitas tunggal untuk
meningkatkan pengelolaan data pasien, mengurangi redundansi, dan
meningkatkan efisiensi layanan kesehatan di Poliklinik Polije. Metode
yang digunakan adalah model pengembangan Waterfall, yang meliputi tahap
analisis kebutuhan, desain, implementasi, dan pengujian sistem. Hasil
dari penelitian menunjukkan bahwa sistem EMR yang dikembangkan mampu
memudahkan akses riwayat pasien, diagnosis yang terhubung dengan ICD-10,
serta catatan terapi melalui identifikasi RFID, sehingga dapat
mempercepat proses pendaftaran, mengurangi kesalahan, dan meningkatkan
kualitas layanan kesehatan secara keseluruhan sesuai dengan regulasi
kesehatan di Indonesia.

Penelitian yang dilakukan saat ini yaitu merancang dan membangun Sistem
Layanan Manajemen Pasien pada PhysioNet berbasis Android. Sistem ini
dikembangkan secara *native* menggunakan bahasa pemrograman Kotlin dan
Android Studio guna menjamin performa dan stabilitas aplikasi yang
tinggi. Untuk pengelolaan basis data, penelitian ini memanfaatkan
Supabase sebagai *Backend-as-a-Service* (BaaS) yang memungkinkan
sinkronisasi data secara *real-time* dan penyimpanan berbasis *cloud*
yang aman. Fitur yang dibangun mencakup manajemen data master pasien,
pencatatan rekam medis digital, manajemen jadwal terapi, serta sistem
pelaporan (*reporting*) untuk evaluasi performa layanan. Implementasi
ini berfokus pada penyediaan platform yang mengakomodasi alur kerja
dokumentasi klinis fisioterapi yang bersifat repetitif dan progresif
langsung melalui perangkat *mobile*. Hal ini memberikan fleksibilitas
bagi terapis tunggal di PhysioNet untuk melakukan sinkronisasi data
rekam medis secara instan di lokasi tindakan, sekaligus mengatasi
keterbatasan aksesibilitas dan mobilitas yang sering ditemui pada sistem
manajemen pasien berbasis *desktop* maupun pencatatan manual.

## Fisioterapi

Fisioterapi adalah profesi kesehatan yang berfokus pada pengoptimalan
kualitas hidup masyarakat melalui upaya mengembangkan, memelihara, dan
memulihkan gerak serta fungsi tubuh sepanjang daur kehidupan. Pelayanan
ini diberikan kepada individu atau kelompok yang mengalami gangguan
gerak dan fungsi tubuh akibat faktor penuaan, cedera, penyakit, gangguan
fisik, maupun faktor lingkungan. Standar pelayanan fisioterapi mandiri
saat ini harus merujuk pada regulasi nasional yang menekankan pada
kompetensi terapis dalam melakukan asuhan secara profesional dan
bertanggung jawab guna menjamin keselamatan pasien (Handayani and Siwi,
2024).

Kualitas pelayanan dalam praktik mandiri fisioterapi juga memiliki
hubungan yang sangat erat dengan tingkat kepuasan dan kepercayaan pasien
terhadap proses rehabilitasi yang dijalani. Kualitas ini diukur melalui
dimensi keandalan, ketanggapan, serta jaminan keamanan dalam setiap
tindakan medis yang diberikan oleh terapis. Oleh karena itu, modernisasi
layanan melalui dukungan teknologi informasi menjadi sangat relevan
untuk memudahkan aksesibilitas layanan serta mempercepat proses
pemulihan pasien melalui pengelolaan jadwal dan rekam medis yang lebih
efisien (ANINDYA, UTAMI and RAHMANTO, 2023).

## Rekam Medis Elektronik

Rekam Medis Elektronik (RME) merupakan sistem penyimpanan data kesehatan
pasien dalam format digital yang mencakup identitas, riwayat medis,
serta hasil pemeriksaan secara terintegrasi. Implementasi RME di
Indonesia saat ini berlandaskan pada Peraturan Menteri Kesehatan (PMK)
Nomor 24 Tahun 2022, yang mewajibkan seluruh fasilitas pelayanan
kesehatan untuk melakukan transisi dari rekam medis manual ke sistem
elektronik guna menciptakan interoperabilitas data kesehatan nasional
melalui platform SatuSehat (Mujtahidah and Istiqamah, 2025).

Penerapan RME terbukti memberikan dampak positif yang signifikan
terhadap efektivitas dan kualitas pelayanan kesehatan. Dengan adanya
sistem digital, tenaga medis dapat mengakses informasi pasien dengan
lebih cepat dan akurat, mengurangi waktu tunggu administrasi, serta
meminimalisir risiko kesalahan medis (medical error) akibat dokumentasi
yang tidak lengkap atau sulit dibaca. Hal ini secara langsung
meningkatkan efisiensi operasional di fasilitas kesehatan dan memberikan
jaminan keamanan data yang lebih baik dibandingkan dengan pengarsipan
fisik konvensional (Siregar, 2024).

Meskipun memberikan banyak keuntungan, implementasi RME menghadapi
berbagai tantangan, mulai dari kesiapan infrastruktur teknologi
informasi, ketersediaan sumber daya manusia yang terampil, hingga aspek
perlindungan privasi data pasien. Oleh karena itu, diperlukan strategi
pengembangan sistem yang komprehensif agar transformasi digital di
sektor kesehatan dapat berjalan optimal sesuai dengan kerangka hukum dan
standar keamanan yang ditetapkan pemerintah (Damayanti, Adiputra and
Pradnyantara, 2025)

## Bahasa Pemrograman Kotlin

Kotlin adalah bahasa pemrograman modern yang bersifat statically typed
dan dirancang untuk berjalan di atas Java Virtual Machine (JVM). Sejak
ditetapkan sebagai bahasa utama (first-class language) oleh Google untuk
pengembangan Android, Kotlin menawarkan berbagai fitur mutakhir yang
bertujuan untuk menyederhanakan proses penulisan kode. Penggunaan Kotlin
terbukti memberikan keunggulan dalam manajemen memori dan efisiensi
kinerja dibandingkan dengan bahasa pemrograman tradisional seperti Java,
menjadikannya pilihan ideal untuk membangun aplikasi mobile yang
responsif dan hemat sumber daya (Sanjaya and Susilo, 2024)

Salah satu fitur paling signifikan yang dimiliki oleh Kotlin adalah Null
Safety, yang secara eksplisit membedakan antara variabel yang boleh
bernilai kosong (nullable) dan yang tidak (non-nullable). Fitur ini
sangat krusial dalam pengembangan aplikasi kesehatan karena mampu
mencegah terjadinya NullPointerException yang sering menjadi penyebab
utama aplikasi berhenti secara mendadak (force close). Dengan stabilitas
sistem yang lebih terjamin, pengembang dapat fokus pada peningkatan
fungsionalitas aplikasi dan kualitas layanan tanpa terganggu oleh
kesalahan-kesalahan pemrograman yang bersifat umum (Priharsanto and
Ajhari, 2024).

Dalam konteks pengembangan aplikasi manajemen rumah sakit atau klinik,
penggunaan Native Android berbasis Kotlin menunjukkan efisiensi
penggunaan sumber daya perangkat yang lebih unggul jika dibandingkan
dengan kerangka kerja lintas platform. Hal ini memungkinkan terapis atau
tenaga medis untuk menjalankan aplikasi secara lancar pada berbagai
spesifikasi perangkat Android tanpa mengalami kendala performa. Dukungan
penuh dari Google serta ekosistem yang matang membuat Kotlin terus
berkembang sebagai teknologi kunci dalam mendukung digitalisasi layanan
kesehatan di era modern (Cendekia, Kharisma and Priyambadha, 2025).

## Android Studio

Android Studio merupakan Integrated Development Environment (IDE) resmi
yang disediakan oleh Google untuk memfasilitasi pengembangan aplikasi
pada platform Android. IDE ini dibangun di atas IntelliJ IDEA dan
menyertakan berbagai fitur pengembang yang canggih untuk membantu
mempercepat proses pembuatan aplikasi yang stabil dan berkualitas
tinggi. Sebagai perangkat lunak standar industri, Android Studio
mendukung penuh penulisan kode menggunakan bahasa pemrograman modern
seperti Kotlin dan menyediakan lingkungan kerja yang terintegrasi untuk
mengelola seluruh siklus hidup aplikasi (Safiya, 2025)

Platform ini menawarkan sistem pembuatan aplikasi berbasis Gradle yang
fleksibel, yang memungkinkan pengembang untuk menyesuaikan konfigurasi
proyek sesuai kebutuhan perangkat keras yang berbeda. Selain itu,
Android Studio dilengkapi dengan emulator yang sangat responsif,
sehingga pengembang dapat mensimulasikan berbagai versi sistem operasi
Android dan ukuran layar tanpa memerlukan banyak perangkat fisik.
Fitur-fitur ini sangat mendukung efisiensi dalam pembangunan sistem
informasi manajemen atau rekam medis digital yang memerlukan pengujian
intensif pada fungsionalitas antarmuka penggunanya (Ghazalba and
Anggara, 2024).

## Supabase 

Supabase adalah platform Backend-as-a-Service (BaaS) sumber terbuka
(open source) yang menyediakan layanan basis data PostgreSQL,
autentikasi, penyimpanan file, dan API secara instan. Sebagai alternatif
dari Firebase, Supabase menawarkan keunggulan dalam hal fleksibilitas
relasional SQL yang kuat, namun tetap menyajikan kemudahan penggunaan
melalui antarmuka dasbor yang intuitif. Fitur ini memungkinkan
pengembang aplikasi untuk membangun sistem backend yang aman dan
berskala besar tanpa perlu mengelola infrastruktur server secara manual,
sehingga sangat cocok untuk pengembangan aplikasi startup atau sistem
informasi manajemen (Muliada, Paramitha and Purnama, 2024).

Salah satu fitur kunci Supabase yang sangat relevan untuk aplikasi
kesehatan adalah kemampuan Realtime Subscriptions. Fitur ini
memungkinkan data yang tersimpan di database untuk disinkronisasi secara
langsung ke aplikasi klien (Android) segera setelah terjadi perubahan
(seperti penambahan pasien baru atau update rekam medis). Hal ini
memastikan bahwa seluruh staf medis yang menggunakan aplikasi selalu
melihat data terbaru tanpa perlu melakukan refresh halaman, yang secara
signifikan meningkatkan efisiensi koordinasi tim medis (Romero *et al.*,
2023)

Dari sisi keamanan, Supabase menerapkan Row Level Security (RLS) pada
PostgreSQL, yang memungkinkan pengembang mendefinisikan aturan akses
data secara granular untuk setiap pengguna. Mekanisme ini menjamin bahwa
rekam medis pasien hanya dapat diakses oleh pihak yang berwenang
(seperti dokter atau terapis terkait), sesuai dengan standar privasi
data kesehatan. Selain itu, ketersediaan SDK (Software Development Kit)
untuk berbagai bahasa pemrograman, termasuk Flutter dan Kotlin,
menjadikan proses integrasi Supabase ke dalam aplikasi mobile menjadi
sangat cepat dan efisien (Phan and Yuricha, 2023).

## Figma

Figma adalah perangkat lunak desain berbasis cloud yang digunakan untuk
merancang antarmuka pengguna (User Interface) dan pengalaman pengguna
(User Experience) secara kolaboratif. Dalam pengembangan aplikasi rekam
medis, penggunaan Figma sangat penting sebagai langkah awal untuk
merancang alur dan visualisasi sistem sebelum aplikasi tersebut mulai
dibangun menggunakan perangkat lunak pengembangan seperti Android
Studio.

Pemanfaatan Figma memungkinkan pengembang untuk membuat prototipe
interaktif yang dapat mensimulasikan fungsi manajemen pasien dan rekam
medis elektronik berbasis smartphone. Perancangan yang matang melalui
alat ini bertujuan untuk memastikan bahwa aplikasi yang dihasilkan
nantinya memiliki tata letak yang ergonomis sehingga mampu meningkatkan
efisiensi dan kualitas pelayanan kesehatan di rumah sakit maupun klinik.

Selain itu, Figma memfasilitasi proses verifikasi desain yang melibatkan
pengguna secara langsung untuk mengevaluasi fitur, tipografi, dan tata
letak halaman dashboard. Melalui tahap perancangan antarmuka yang
mendetail ini, pengembang dapat mengatasi permasalahan pada sistem
manual dengan menciptakan solusi digital yang lebih terstruktur dan
mudah dioperasikan oleh tenaga medis dalam mencatat rekam medis pasien.

## Black Box Testing

Black Box Testing merupakan metode pengujian perangkat lunak yang
berfokus pada pemeriksaan fungsionalitas sistem tanpa harus mengetahui
struktur kode internal atau detail implementasi teknis di dalamnya.
Pengujian ini dilakukan dengan memberikan berbagai variasi data masukan
(input) ke dalam sistem dan memantau keluaran (output) yang dihasilkan
guna memastikan bahwa aplikasi berjalan sesuai dengan dokumen
spesifikasi kebutuhan yang telah ditetapkan. Karena pendekatan ini
memandang sistem sebagai \"kotak hitam\" yang hanya terlihat
antarmukanya, pengujian dapat dilakukan secara objektif dari perspektif
pengguna akhir untuk menemukan kesalahan fungsi, kendala pada antarmuka
pengguna, serta inkonsistensi dalam alur proses bisnis.

Dalam implementasinya, Black Box Testing sering menggunakan teknik
sistematis seperti Equivalence Partitioning untuk membagi domain masukan
menjadi kelas-kelas data yang mewakili kondisi valid dan tidak valid,
serta Boundary Value Analysis untuk menguji akurasi sistem pada nilai
batas minimum maupun maksimum. Keunggulan utama metode ini terletak pada
kemampuannya untuk mendeteksi celah antara fungsionalitas yang
diharapkan dengan hasil nyata tanpa dipengaruhi oleh bias pengembangan
kode. Dengan menerapkan pengujian ini secara menyeluruh dalam siklus
pengembangan, pengembang dapat menjamin kualitas dan reliabilitas
sistem, sehingga risiko terjadinya kegagalan fungsi saat aplikasi
digunakan secara luas dapat diminimalisir secara efektif (Ahsa *et al.*,
2023)

## Metode Waterfall

Metodologi Waterfall merupakan salah satu model pengembangan sistem
informasi yang menjadi bagian dari *System Development Life Cycle*
(SDLC). Karakteristik utama dari metode ini adalah proses pengerjaannya
yang dilakukan secara berurutan atau sekuensial. Dalam model ini, sebuah
tahapan tidak dapat dikerjakan apabila tahapan sebelumnya belum
diselesaikan sepenuhnya, sehingga alur pengembangannya mengalir ke bawah
seperti air terjun. Karena sifatnya yang terstruktur dan sistematis,
metode ini dianggap sederhana dan mudah untuk dimengerti oleh pengembang
maupun pemangku kepentingan. (Ramadhan, Haniva and Suharso, 2023).
