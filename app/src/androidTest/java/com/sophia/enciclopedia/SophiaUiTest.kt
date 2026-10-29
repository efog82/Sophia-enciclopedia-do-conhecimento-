package com.sophia.enciclopedia

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.FileInputStream

@RunWith(AndroidJUnit4::class)
class SophiaUiTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private fun ready(){ui.waitUntil(15000){ui.onAllNodesWithTag("home-screen").fetchSemanticsNodes().isNotEmpty() || ui.onAllNodesWithTag("library-screen").fetchSemanticsNodes().isNotEmpty()}}
    private fun awaitTag(tag:String,present:Boolean=true){ui.waitUntil(15000){ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()==present}}
    private fun awaitText(text:String,present:Boolean=true){ui.waitUntil(15000){ui.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()==present}}
    private fun nav(id:String){ui.onNodeWithTag("nav-$id").performClick();ui.waitForIdle()}
    private fun shell(command:String){InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use{fd->FileInputStream(fd.fileDescriptor).use{it.readBytes()}}}
    private fun shot(name:String){ui.waitForIdle();shell("mkdir -p /sdcard/Download/Sophia-Test");shell("screencap -p /sdcard/Download/Sophia-Test/$name.png")}

    @Test fun completeOfflineJourney(){
        ready();nav("HOME");ui.onNodeWithTag("theme-settings").performClick();ui.onNodeWithTag("theme-LIGHT").performClick();ui.waitUntil(15000){ui.activity.model.state.value.theme==com.sophia.domain.ThemeMode.LIGHT};shot("01-inicio")
        assertEquals(PackageManager.PERMISSION_DENIED,ui.activity.checkSelfPermission(Manifest.permission.INTERNET))
        nav("LIBRARY");ui.onNodeWithTag("library-search").performTextReplacement("livro dos espiritos");ui.onNodeWithTag("library-search").performImeAction()
        ui.waitUntil(15000){ui.activity.model.state.value.library.size==1};ui.onNodeWithTag("library-screen").performScrollToNode(hasTestTag("library-count"));awaitText("1 obra encontrada");ui.onNodeWithTag("library-count").assertTextEquals("1 obra encontrada")
        ui.onNodeWithTag("library-screen").performScrollToNode(hasTestTag("book-spirits"));ui.onNodeWithTag("book-spirits").performClick();ui.onNodeWithTag("book-details").assertExists()
        ui.onNodeWithTag("detail-favorite").performScrollTo().performClick()
        awaitText("Remover dos favoritos");ui.onNodeWithText("Remover dos favoritos").assertExists()
        ui.onNodeWithText("Fechar detalhes").performScrollTo().performClick()
        ui.activityRule.scenario.recreate();awaitTag("library-screen");ui.onNodeWithTag("library-search").assertTextContains("livro dos espiritos")
        ui.onNodeWithTag("favorite-spirits").assertContentDescriptionEquals("Remover dos favoritos: O Livro dos Espíritos")
        shot("02-biblioteca")
        nav("RESEARCH");ui.onNodeWithTag("research-search").performTextInput("dhammapada");ui.onNodeWithTag("research-search").performImeAction();ui.waitUntil(15000){ui.activity.model.state.value.research.size==1};ui.onNodeWithTag("research-screen").performScrollToNode(hasText("1 resultados de metadados"));awaitText("1 resultados de metadados");ui.onNodeWithText("1 resultados de metadados").assertExists()
        ui.onNodeWithTag("research-screen").performScrollToIndex(0);ui.onAllNodesWithText("Todos")[0].performClick();ui.onNodeWithText("Allan Kardec").performScrollTo().performClick();ui.waitUntil(15000){ui.activity.model.state.value.research.isEmpty()};ui.onNodeWithTag("research-screen").performScrollToNode(hasText("Não encontrámos correspondências"));awaitText("Não encontrámos correspondências")
        ui.onNodeWithText("Limpar").performScrollTo().performClick();ui.onNodeWithTag("research-screen").performScrollToNode(hasText("Por onde queres começar?"));awaitText("Por onde queres começar?")
        nav("AI");ui.onNodeWithText("Em desenvolvimento · fase futura").assertExists();shot("03-assistente")
        nav("NOTES");ui.onNodeWithTag("new-note").performClick()
        ui.onNodeWithTag("save-note").performScrollTo().performClick();awaitTag("note-error")
        ui.onNodeWithTag("note-title").performScrollTo()
        ui.onNodeWithTag("note-title").performTextInput("Reflexão offline")
        ui.onNodeWithTag("note-body").performTextInput("Comparar fontes e tradições.")
        ui.onNodeWithTag("save-note").performScrollTo().performClick();awaitTag("note-editor",false)
        ui.onNodeWithText("Reflexão offline").performClick()
        ui.onNodeWithTag("note-body").performTextReplacement("Apontamento revisto.")
        ui.onNodeWithTag("save-note").performScrollTo().performClick();awaitTag("note-editor",false)
        ui.activityRule.scenario.recreate();awaitTag("notes-screen");awaitText("Apontamento revisto.");ui.onNodeWithText("Apontamento revisto.").assertExists();shot("04-caderno")
        ui.onNodeWithContentDescription("Eliminar nota: Reflexão offline").performClick()
        ui.onNodeWithText("Cancelar").performClick();ui.onNodeWithText("Reflexão offline").assertExists()
        ui.onNodeWithContentDescription("Eliminar nota: Reflexão offline").performClick()
        ui.onNodeWithTag("confirm-delete-note").performClick();awaitText("Reflexão offline",false);ui.onNodeWithText("Reflexão offline").assertDoesNotExist()
        ui.onNodeWithTag("new-note").performClick();ui.onNodeWithTag("note-title").performTextInput("Rascunho")
        ui.activityRule.scenario.recreate();awaitTag("note-editor");ui.onNodeWithTag("note-title").assertTextContains("Rascunho")
        ui.onNodeWithTag("cancel-note").performScrollTo().performClick();ui.onNodeWithTag("confirm-discard").performClick()
        ui.onNodeWithText("Rascunho").assertDoesNotExist()
        nav("HOME");ui.onNodeWithTag("theme-settings").performClick();ui.onNodeWithTag("theme-DARK").performClick();ui.waitUntil(15000){ui.activity.model.state.value.theme==com.sophia.domain.ThemeMode.DARK};shot("05-inicio-escuro")
        ui.activityRule.scenario.recreate();awaitTag("home-screen");ui.onNodeWithTag("home-screen").assertExists()
        ui.onNodeWithTag("theme-settings").performClick();ui.onNodeWithText("✓  Escuro").assertExists();ui.onNodeWithText("Fechar").performClick()
        ui.onNodeWithTag("home-screen").performScrollToNode(hasText("Ocultismo"));ui.onNodeWithText("Ocultismo").performClick();ui.onNodeWithTag("library-screen").performScrollToNode(hasText("Nenhuma obra encontrada"));awaitText("Nenhuma obra encontrada")
        ui.onNodeWithText("Limpar filtros").performClick();awaitText("${ui.activity.model.state.value.books.size} obras encontradas")
    }
}
