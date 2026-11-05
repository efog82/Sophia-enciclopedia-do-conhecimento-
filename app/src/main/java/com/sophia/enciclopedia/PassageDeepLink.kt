package com.sophia.enciclopedia

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Ponte entre a notificação da passagem diária e a interface: a MainActivity regista aqui
 * o livro/capítulo pedidos e o SophiaApp consome o pedido (instala o livro se for preciso
 * e abre o leitor na unidade certa).
 */
object PassageDeepLink {
    const val EXTRA_BOOK="passageBookId"
    const val EXTRA_UNIT="passageUnit"
    val pending=MutableStateFlow<Pair<String,Int>?>(null)
    fun offer(bookId:String?,unit:Int){if(!bookId.isNullOrBlank())pending.value=bookId to unit.coerceAtLeast(0)}
    fun clear(){pending.value=null}
}
