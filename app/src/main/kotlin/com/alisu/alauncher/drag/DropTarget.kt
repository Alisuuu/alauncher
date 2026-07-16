package com.alisu.alauncher.drag

import com.alisu.alauncher.model.AppInfo

/**
 * Contrato que qualquer área do launcher que aceite drops deve implementar.
 *
 * Implementado por:
 *  - WorkspaceLayout → aceita Apps, Pastas, Widgets
 *  - DockLayout      → aceita apenas Apps e Pastas (não aceita Widgets)
 */
interface DropTarget {

    /**
     * Retorna true se esta área aceita o item sendo arrastado.
     * Chamado continuamente durante o drag para feedback visual.
     */
    fun accepts(item: AppInfo): Boolean

    /**
     * Chamado quando o usuário solta o dedo e o item deve ser inserido aqui.
     * Só é chamado se [accepts] retornou true para este item.
     */
    fun onDrop(item: AppInfo, x: Float, y: Float)

    /**
     * Retorna true se as coordenadas de tela absolutas (rawX, rawY)
     * estão dentro dos limites desta área.
     */
    fun containsPoint(x: Float, y: Float): Boolean

    /** Chamado quando o ícone entra nos limites desta área. */
    fun onDragEnter(item: AppInfo) {}

    /** Chamado quando o ícone sai dos limites desta área. */
    fun onDragExit(item: AppInfo) {}

    /** Chamado continuamente enquanto o ícone se move dentro desta área. */
    fun onDragOver(item: AppInfo, x: Float, y: Float) {}
}
