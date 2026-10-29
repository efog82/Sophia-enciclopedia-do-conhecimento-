package com.sophia.enciclopedia

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.util.zip.*

@RunWith(AndroidJUnit4::class)
class ReaderFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val device get()=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun awaitTag(tag:String,present:Boolean=true){ui.waitUntil(20000){ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()==present}}
    private fun screenshot(name:String){ui.waitForIdle();listOf("mkdir -p /sdcard/Download/Sophia-Test","screencap -p /sdcard/Download/Sophia-Test/$name.png").forEach{command->InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use{fd->FileInputStream(fd.fileDescriptor).use{it.readBytes()}}}}
    private fun fixtures(){
        val dir=File(InstrumentationRegistry.getInstrumentation().context.filesDir,"reader-fixtures").apply{mkdirs()}
        File(dir,"01-Sophia.txt").writeText("Texto técnico original Sophia, criado apenas para ensaio.\n".repeat(300))
        ZipOutputStream(File(dir,"02-Sophia.epub").outputStream()).use{zip->mapOf("mimetype" to "application/epub+zip","META-INF/container.xml" to "<container><rootfiles><rootfile full-path='OPS/book.opf'/></rootfiles></container>","OPS/book.opf" to "<package><metadata><title>EPUB técnico Sophia</title><creator>Autor de ensaio</creator><language>pt</language></metadata><manifest><item id='a' href='a.xhtml' media-type='application/xhtml+xml'/><item id='b' href='b.xhtml' media-type='application/xhtml+xml'/></manifest><spine><itemref idref='a'/><itemref idref='b'/></spine></package>","OPS/a.xhtml" to "<html><body><h1>Primeiro recurso EPUB</h1><p>Conteúdo original de ensaio EPUB.</p></body></html>","OPS/b.xhtml" to "<html><body><h1>Segundo recurso EPUB</h1><p>Passagem de teste para notas.</p></body></html>").forEach{(name,text)->zip.putNextEntry(ZipEntry(name));zip.write(text.toByteArray());zip.closeEntry()}}
        PdfDocument().use{pdf->repeat(2){i->val page=pdf.startPage(PdfDocument.PageInfo.Builder(500,700,i+1).create());page.canvas.drawText("Sophia - PDF de teste ${i+1}",30f,90f,Paint().apply{textSize=22f});pdf.finishPage(page)};File(dir,"03-Sophia.pdf").outputStream().use{pdf.writeTo(it)}}
    }
    private fun pick(name:String){
        ui.onNodeWithTag("import-file").performScrollTo().performClick()
        val roots=device.wait(Until.findObject(By.desc("Show roots")),10000)?:device.wait(Until.findObject(By.desc("Mostrar raízes")),2000)
        roots?.click()
        val root=device.wait(Until.findObject(By.text("Sophia-Teste")),10000)?:error("O seletor Android não apresentou a fonte técnica de teste")
        root.click()
        val item=device.wait(Until.findObject(By.text(name)),10000)?:error("Ficheiro de ensaio não visível no seletor: $name")
        item.click();awaitTag("confirm-import")
        ui.onNodeWithTag("import-rights").performScrollTo().performClick();ui.onNodeWithTag("confirm-import").performScrollTo().performClick()
        ui.waitUntil(20000){ui.onAllNodesWithText("Ficheiro importado. Já pode ser lido offline.").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("Fechar").performClick();awaitTag("book-details")
    }
    private fun openImported(name:String):String{
        ui.waitUntil(15000){ui.activity.libraryModel.state.value.data.files.any{it.displayName==name}}
        val id=ui.activity.libraryModel.state.value.data.files.first{it.displayName==name}.id
        ui.onNodeWithTag("file-$id").performScrollTo().performClick();awaitTag("reader-screen");ui.waitUntil(20000){ui.onAllNodesWithTag("reader-next").fetchSemanticsNodes().isNotEmpty()};return id
    }
    @Test fun safImportAndReadingAllThreeFormats(){
        fixtures();awaitTag("home-screen")
        runBlocking{(ui.activity.application as SophiaApplication).container.preferences.setReaderFontSize(20)}
        ui.onNodeWithTag("open-traditions").performScrollTo().performClick();ui.onNodeWithTag("tradition-hinduism").performScrollTo().performClick();ui.onNodeWithText("Rigveda").performScrollTo().assertExists();ui.onNodeWithText("Fechar").performScrollTo().performClick()
        ui.onNodeWithTag("nav-LIBRARY").performClick();pick("01-Sophia.txt");val txtId=openImported("01-Sophia.txt");ui.onNodeWithTag("txt-content").assertExists();ui.onNodeWithTag("reader-next").performClick();screenshot("06-leitor-txt");ui.onNodeWithTag("close-reader").performClick();awaitTag("book-details");ui.waitUntil(10000){ui.activity.libraryModel.state.value.data.files.first{it.id==txtId}.location?.unit==1};ui.onNodeWithText("Fechar detalhes").performScrollTo().performClick()
        ui.onNodeWithTag("library-screen").performScrollToIndex(0);pick("02-Sophia.epub");val epubId=openImported("02-Sophia.epub");awaitTag("epub-content")
        assertNotNull("O WebView deve apresentar texto real do EPUB",device.wait(Until.findObject(By.textContains("Primeiro recurso EPUB")),15000))
        ui.onNodeWithTag("font-plus").performClick();ui.waitUntil(15000){ui.onAllNodesWithTag("reader-font").fetchSemanticsNodes().any{node->node.config[androidx.compose.ui.semantics.SemanticsProperties.Text].any{it.text=="22"}}}
        ui.onNodeWithTag("reader-next").performClick();ui.onNodeWithTag("add-bookmark").performClick();ui.waitUntil(10000){ui.onAllNodesWithText("Marcador guardado.").fetchSemanticsNodes().isNotEmpty()};ui.onNodeWithText("Fechar").performClick()
        ui.onNodeWithTag("open-bookmarks").performClick();ui.onNodeWithText("Segundo recurso EPUB").performClick()
        ui.onNodeWithTag("reader-note").performClick();ui.onNodeWithTag("passage-note-title").performTextReplacement("Nota de passagem EPUB");ui.onNodeWithTag("passage-note-body").performTextInput("Estudo pessoal fundamentado no ficheiro.");ui.onNodeWithTag("save-passage-note").performScrollTo().performClick();awaitTag("save-passage-note",false);screenshot("07-leitor-epub")
        ui.onNodeWithTag("close-reader").performClick();awaitTag("book-details");ui.waitUntil(10000){ui.activity.libraryModel.state.value.data.files.first{it.id==epubId}.location?.unit==1};ui.onNodeWithText("Fechar detalhes").performScrollTo().performClick()
        ui.onNodeWithTag("library-screen").performScrollToIndex(0);pick("03-Sophia.pdf");openImported("03-Sophia.pdf");ui.waitUntil(20000){ui.onAllNodesWithContentDescription("Página PDF").fetchSemanticsNodes().isNotEmpty()};ui.onNodeWithTag("reader-next").performClick();ui.waitUntil(20000){ui.onAllNodesWithContentDescription("Página PDF").fetchSemanticsNodes().isNotEmpty()};screenshot("08-leitor-pdf");ui.onNodeWithTag("close-reader").performClick();awaitTag("book-details");ui.onNodeWithText("Fechar detalhes").performScrollTo().performClick()
        ui.onNodeWithTag("nav-NOTES").performClick();ui.waitUntil(10000){ui.onAllNodesWithText("Nota de passagem EPUB").fetchSemanticsNodes().isNotEmpty()};ui.onNodeWithText("Nota de passagem EPUB").performScrollTo().performClick();ui.onNodeWithText("Ler passagem associada").performScrollTo().performClick();awaitTag("reader-screen");ui.onNodeWithTag("reader-index").assertTextContains("2/2",substring=true);ui.onNodeWithTag("close-reader").performClick();awaitTag("note-editor");ui.onNodeWithTag("cancel-note").performScrollTo().performClick()
    }
}
