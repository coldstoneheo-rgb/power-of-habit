package com.example.powerofhabit.data.local

import java.io.File
import java.io.FileInputStream

/**
 * SQLite 파일 헤더(첫 100바이트)만 읽는 도우미. DB를 열지 않고 "SQLite 파일인가 / 어느 스키마 버전인가 / WAL 모드인가"를 본다.
 * Drive 복원과 옛 앱 DB 가져오기가 함께 쓴다. 헤더 레이아웃: https://www.sqlite.org/fileformat.html#the_database_header
 */
object SqliteFileHeader {
    /** 오프셋 0~15. 마지막 바이트는 NUL — 소스에 제어문자를 직접 넣지 않도록 이스케이프로 쓴다. */
    const val MAGIC = "SQLite format 3\u0000"
    private const val HEADER_SIZE = 100

    private fun read(file: File): ByteArray? {
        if (file.length() < HEADER_SIZE) return null
        val head = ByteArray(HEADER_SIZE)
        FileInputStream(file).use { if (it.read(head) != HEADER_SIZE) return null }
        return head
    }

    fun isSqlite(file: File): Boolean {
        val head = read(file) ?: return false
        return head.copyOfRange(0, 16).contentEquals(MAGIC.toByteArray(Charsets.ISO_8859_1))
    }

    /**
     * 오프셋 60~63(big-endian) = PRAGMA user_version(Room 스키마 버전). **헤더 기준**이라 체크포인트 안 된 WAL에 더 새 값이 있을 수 있다 —
     * 열 수 있는 상황이면 `PRAGMA user_version`을 우선하고, 이 값은 열기 전 빠른 거부용으로만 쓴다.
     */
    fun userVersion(file: File): Int {
        val h = read(file) ?: return 0
        return ((h[60].toInt() and 0xFF) shl 24) or ((h[61].toInt() and 0xFF) shl 16) or
            ((h[62].toInt() and 0xFF) shl 8) or (h[63].toInt() and 0xFF)
    }

    /** 오프셋 18·19(파일 형식 읽기/쓰기 버전)가 2면 WAL 저널 모드. Room 기본값이 WAL이므로 옛 앱 DB는 대개 true. */
    fun isWalMode(file: File): Boolean {
        val h = read(file) ?: return false
        return h[18].toInt() == 2 && h[19].toInt() == 2
    }
}
