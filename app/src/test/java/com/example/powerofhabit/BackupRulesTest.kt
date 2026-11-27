package com.example.powerofhabit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * OS 자동 백업 · 기기 이전에서 Google 로그인 라이브러리 저장소를 뺀다.
 * 저장소 이름 근거: play-services-base signin.internal.Storage → getSharedPreferences("com.google.android.gms.signin")
 * (docs/handoffs/TASK-2026-1125-habit-prep-before-user-answers-REPORT.md 바이트코드 판독).
 * 습관 DB · 앱 설정은 계속 백업된다(include 없이 exclude만 둠 = 나머지 전부 포함).
 */
class BackupRulesTest {
    private val signInPrefs = "com.google.android.gms.signin.xml"
    private val res = File("src/main/res/xml")

    private fun parse(f: File): Element =
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(f).documentElement

    private fun excludes(parent: Element): List<Pair<String, String>> {
        val list = parent.getElementsByTagName("exclude")
        return (0 until list.length).map { list.item(it) as Element }
            .map { it.getAttribute("domain") to it.getAttribute("path") }
    }

    private fun assertOnlySignInExcluded(section: Element, where: String) {
        assertEquals("$where: exclude 목록", listOf("sharedpref" to signInPrefs), excludes(section))
        assertEquals("$where: include가 있으면 DB가 빠질 수 있다", 0, section.getElementsByTagName("include").length)
    }

    @Test
    fun manifestWiresBothRuleFiles() {
        val app = parse(File("src/main/AndroidManifest.xml")).getElementsByTagName("application").item(0) as Element
        val ns = "http://schemas.android.com/apk/res/android"
        assertEquals("true", app.getAttributeNS(ns, "allowBackup"))
        assertEquals("@xml/backup_rules", app.getAttributeNS(ns, "fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", app.getAttributeNS(ns, "dataExtractionRules"))
    }

    @Test
    fun legacyFullBackupExcludesOnlySignInStore() {
        val root = parse(File(res, "backup_rules.xml"))
        assertEquals("full-backup-content", root.tagName)
        assertOnlySignInExcluded(root, "backup_rules")
    }

    @Test
    fun dataExtractionExcludesSignInStoreForCloudAndDeviceTransfer() {
        val root = parse(File(res, "data_extraction_rules.xml"))
        assertEquals("data-extraction-rules", root.tagName)
        for (tag in listOf("cloud-backup", "device-transfer")) {
            val sections = root.getElementsByTagName(tag)
            assertTrue("$tag 섹션이 있어야 한다", sections.length == 1)
            assertOnlySignInExcluded(sections.item(0) as Element, tag)
        }
    }
}
