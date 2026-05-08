# BAB I PENDAHULUAN {#bab-i-pendahuluan .unnumbered}

Bab I memaparkan dasar penelitian dari laporan tugas akhir. Bab I
membahas mengenai latar belakang, rumusan masalah, tujuan, manfaat,
batasan masalah dari penelitian yang dilakukan, dan sistematika
penulisan laporan tugas akhir.

## Latar Belakang

Kemajuan teknologi informasi kesehatan mendorong transformasi digital yang signifikan untuk meningkatkan mutu pelayanan. Sejalan dengan Peraturan Menteri Kesehatan RI No. 24 Tahun 2022 yang mewajibkan seluruh fasilitas kesehatan menerapkan Rekam Medis Elektronik (RME). Implementasi digitalisasi pada pelayanan fisioterapi masih terkendala oleh dominasi pencatatan manual. Sistem berbasis kertas terbukti kurang efektif mengingat tingginya risiko kerusakan dan kehilangan data, yang pada akhirnya memperlambat akses rekam medis untuk evaluasi perkembangan pasien (Firdonsyah and Dewi, 2024).
Penelitian sebelumnya oleh (Khabib, Rozi and Rosadi, 2023) telah mencoba mengatasi masalah serupa dengan mengembangkan aplikasi rekam medis berbasis Android untuk Puskesmas. Studi menunjukkan bahwa penggunaan perangkat mobile dapat secara signifikan meningkatkan kerja staf dibandingkan dengan sistem pencatatan hybrid yang masih menggunakan dokumen fisik. Penelitian oleh (Binti et al., 2020) mengembangkan sistem rekam medis berbasis web menggunakan framework Laravel. Hasil penelitian menekankan bahwa digitalisasi rekam medis sangat krusial untuk mengurangi risiko kerusakan data yang sering terjadi pada klinik swasta yang sedang berkembang.
Berdasarkan kajian literatur sebelumnya, pengembangan sistem informasi kesehatan masih didominasi oleh platform berbasis web yang sangat bergantung pada stabilitas konektivitas internet. Aplikasi rekam medis yang tersedia di pasaran umumnya dirancang secara general untuk rumah sakit besar, sehingga belum mengakomodasi kebutuhan spesifik layanan fisioterapi. Kenyataannya, masih terdapat kesenjangan ketersediaan aplikasi mobile native untuk alur kerja fisioterapi di PhysioNet, khususnya dalam hal dokumentasi sesi terapi berulang dan fleksibilitas akses data tanpa keterbatasan perangkat komputer meja (desktop).
Kendala operasional akibat pengelolaan data manual di PhysioNet menuntut implementasi solusi teknologi yang mampu mengintegrasikan seluruh layanan, mulai dari pendaftaran pasien, dokumentasi rekam medis, pembuatan jadwal janji temu, riwayat terapi pasien, hingga pemantauan progres klinis dalam satu platform digital terpusat. Pembeda utama penelitian ini dengan penelitian sebelumnya terletak pada integrasi sistem manajemen keuangan yang memanfaatkan Midtrans sebagai payment gateway untuk mendukung pembayaran digital via QRIS, serta implementasi sistem penjadwalan pasien yang dirancang khusus untuk menangani sesi terapi berulang secara otomatis. Pemanfaatan teknologi berbasis Android menjadi solusi strategis karena menawarkan keunggulan berupa mobilitas perangkat serta pengoperasian yang intuitif bagi pengguna. Pemilihan platform Android didasarkan pada dominasi sistem operasi di pasar gawai Indonesia serta kemudahan aksesibilitas bagi tenaga kesehatan. Dengan demikian, sistem berbasis mobile ini memungkinkan terapis untuk mengakses data pasien secara real-time, mencatat perkembangan terapi secara digital, dan mengelola jadwal serta transaksi dengan lebih efisien.


## Rumusan Masalah

Rumusan masalah merupakan pernyataan yang merinci dan mengidentifikasi
terkait latar belakang yang telah dibuat. Berdasarkan latar belakang
penelitian yang telah diuraikan pada sub-bab sebelumnya, maka dapat
dirumuskan beberapa masalah yaitu sebagai berikut.

1.  Bagaimana merancang sistem layanan manajemen pasien berbasis Android
    yang dapat mengatasi keterbatasan sistem pencatatan manual yang
    digunakan di PhysioNet.

2.  Bagaimana mengidentifikasi dan menentukan fitur-fitur yang sesuai
    dengan karakteristik layanan fisioterapi di PhysioNet guna
    memastikan dokumentasi kondisi pasien dapat dilakukan secara
    sistematis.

3.  Bagaimana implementasi sistem digital dapat memberikan peningkatan
    terhadap efisiensi operasional dan kualitas layanan di PhysioNet.

Evaluasi ini penting untuk memastikan bahwa investasi teknologi
memberikan *return value* yang optimal bagi organisasi dan kepuasan
pasien.

## Tujuan Penelitian

Tujuan penelitian merupakan hal yang diinginkan untuk tercapai pada
penelitian yang dilakukan. Tujuan penelitian berdasarkan rumusan masalah
yang telah dipaparkan adalah sebagai berikut.

1.  Merancang dan membangun sistem layanan manajemen pasien berbasis
    Android yang komprehensif pada PhysioNet, dikembangkan menggunakan
    bahasa pemrograman Kotlin dan lingkungan kerja Android Studio untuk
    memastikan kinerja serta stabilitas aplikasi yang optimal.

2.  Membangun fitur pengelolaan data yang mencakup pendaftaran pasien
    baru, digitalisasi rekam medis, riwayat terapi pasien, penjadwalan,
    pelaporan serta penyediaan sistem pencarian data pasien yang
    efisien.

3.  Menganalisis dan mengimplementasikan sistem digital untuk
    meningkatkan efisiensi waktu operasional terapis serta kualitas
    layanan melalui dokumentasi data yang lebih akurat dan terstruktur
    di PhysioNet.

## Manfaat Penelitian 

Penelitian ini diharapkan dapat memberikan kontribusi nyata bagi
PhysioNet melalui peningkatan efisiensi operasional dan keamanan rekam
medis yang sebelumnya dikelola secara manual menjadi sistem digital
terstruktur. Bagi terapis, aplikasi ini bermanfaat untuk mempermudah
dokumentasi sesi terapi dan pemantauan perkembangan pasien melalui
antarmuka yang *user-friendly* tanpa memerlukan keahlian teknis yang
mendalam. Bagi bidang akademis, penelitian ini dapat menjadi referensi
dalam pengembangan aplikasi *health informatics* berbasis Android
menggunakan teknologi Kotlin dan *cloud database*.

## Batasan Penelitian

Agar penelitian ini lebih terarah dan tetap pada ruang lingkup yang
direncanakan, maka ditetapkan batasan masalah sebagai berikut:

1.  Sistem ini dikembangkan khusus untuk satu tipe pengguna (terapis)
    sebagai pengelola data internal di PhysioNet dan tidak menyediakan
    antarmuka khusus untuk pasien (*patient-side app*).

2.  Aplikasi dikembangkan secara native untuk perangkat bergerak dengan
    sistem operasi Android, menggunakan bahasa pemrograman Kotlin dan
    lingkungan pengembangan Android Studio.

3.  Penyimpanan dan pengelolaan data menggunakan Supabase sebagai
    *Backend-as-a-Service* (BaaS) dengan struktur basis data PostgreSQL
    yang berbasis *cloud*.

4.  Fitur utama dibatasi pada pendaftaran pasien, pencatatan rekam medis
    fisioterapi, pengelolaan jadwal terapi, riwayat terapi pasien,
    hingga pemantauan progres klinis dalam satu *platform* digital
    terpusat.

## Sistematika Penulisan

Sistematika penulisan laporan ini disusun dalam lima bab. Adapun
gambaran umum mengenai isi setiap bab dapat di jelaskan sebagai berikut.

**BAB I PENDAHULUAN**

> Bab I memaparkan dasar penelitian dari laporan tugas akhir. Bab I
> membahas mengenai latar belakang, rumusan masalah, tujuan, manfaat,
> batasan masalah dari penelitian yang dilakukan, dan sistematika
> penulisan laporan tugas akhir.

**BAB II TINJAUAN PUSTAKA**

> Bab II merupakan tinjauan Pustaka dari laporan tugas akhir. Bab II
> membahas state of the art, definisi, dan teori-teori yang digunakan
> dalam penelitian. Kumpulan teori tersebut digunakan sebagai penunjang
> dalam pembuatan penelitian tugas akhir.

**BAB III METODOLOGI PENELITIAN**

> Bab III merupakan metodologi penelitian dari laporan tugas akhir. Bab
> III berisi mengenai tempat dan waktu pelaksanaan penelitian, sumber
> data, instrumen pembuatan sistem, metode pengembangan sistem, dan
> perancangan sistem.

**BAB IV HASIL DAN PEMBAHASAN**

> Bab IV merupakan hasil dan pembahasan dari laporan tugas akhir. Bab IV
> membahas mengenai hasil dan pembahasan serta pengujian dari aplikasi
> PhysioNet berbasis android.

**BAB V PENUTUP**

Bab V merupakan penutup dari laporan tugas akhir. Bab V membahas
mengenai kesimpulan dan saran serta masukan yang merujuk pada rumusan
masalah penelitian.
