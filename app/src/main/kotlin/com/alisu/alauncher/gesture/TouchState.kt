package com.alisu.alauncher.gesture

/**
 * Máquina de estados de toque do Alauncher.
 *
 * Transitions:
 *   IDLE ──ACTION_DOWN──► CLICKING
 *   CLICKING ──dx > touchSlop──► SCROLLING
 *   CLICKING ──long press timeout──► DRAGGING
 *   CLICKING ──ACTION_UP──► IDLE  (dispara onClick)
 *   DRAGGING ──ACTION_UP──► IDLE  (dispara onDrop)
 *   IDLE / CLICKING ──popup opened──► POPUP
 *   POPUP ──dismissed──► IDLE
 */
enum class TouchState {
    /** Nenhum toque ativo. Estado padrão. */
    IDLE,

    /** ACTION_DOWN detectado. Aguardando: UP (clique), slop (scroll) ou long press (drag). */
    CLICKING,

    /** O dedo se moveu além do touchSlop antes do long press — é um scroll/swipe. */
    SCROLLING,

    /** Long press confirmado. O ícone flutuante está ativo na DragLayer. */
    DRAGGING,

    /** Um menu de contexto (PopupWindow) está visível. Ignora novos toques. */
    POPUP
}
