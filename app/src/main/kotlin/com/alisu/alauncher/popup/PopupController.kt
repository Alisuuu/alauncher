package com.alisu.alauncher.popup

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.alisu.alauncher.gesture.GestureController
import com.alisu.alauncher.gesture.TouchState

/**
 * Centraliza a abertura e o fechamento de todos os PopupWindows do Alauncher.
 *
 * Garante que um popup NUNCA seja aberto enquanto um drag está em progresso,
 * e atualiza o [GestureController] para que ele bloqueie novos gestos enquanto
 * o menu estiver visível.
 *
 * Uso:
 *   popupController.show(anchorView, rootLayout, x, y) {
 *       buildMenuView()   // retorna a View a ser exibida no popup
 *   }
 */
class PopupController(
    private val gestureController: GestureController
) {
    private var activePopup: PopupWindow? = null

    /**
     * Exibe um [PopupWindow] com o conteúdo construído por [buildContent].
     *
     * @param rootView     View raiz da Activity (para showAtLocation)
     * @param x            Posição X desejada na tela
     * @param y            Posição Y desejada na tela
     * @param buildContent Lambda que constrói e retorna a View do menu
     */
    fun show(
        rootView: View,
        x: Int,
        y: Int,
        buildContent: () -> View
    ) {
        // Bloqueia durante drag ou scroll — evita popup acidental
        if (gestureController.state == TouchState.DRAGGING)  return
        if (gestureController.state == TouchState.SCROLLING) return

        // Fecha popup anterior se houver
        dismiss()

        val contentView = buildContent()
        val popup = PopupWindow(
            contentView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true  // focusable — fecha ao clicar fora
        ).apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            elevation = 24f
            setOnDismissListener {
                activePopup = null
                gestureController.onPopupDismissed()
            }
        }

        activePopup = popup
        gestureController.onPopupOpened()

        try {
            popup.showAtLocation(rootView, Gravity.NO_GRAVITY, x, y)
        } catch (e: Exception) {
            // Fallback: centraliza na tela
            popup.showAtLocation(rootView, Gravity.CENTER, 0, 0)
        }
    }

    /**
     * Exibe o popup ancorado a uma View específica, posicionado acima ou abaixo dela.
     *
     * @param anchorView   View que serve de âncora para o popup
     * @param rootView     View raiz para fallback
     * @param buildContent Lambda que constrói e retorna a View do menu
     * @param popupWidthPx Largura do popup em pixels
     */
    fun showAnchored(
        anchorView: View,
        rootView: View,
        popupWidthPx: Int,
        buildContent: () -> View
    ) {
        if (gestureController.state == TouchState.DRAGGING)  return
        if (gestureController.state == TouchState.SCROLLING) return

        dismiss()

        val contentView = buildContent()

        // Mede o conteúdo para calcular posição ideal
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupHeight = contentView.measuredHeight

        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val viewX   = location[0]
        val viewY   = location[1]
        val viewW   = anchorView.width
        val viewH   = anchorView.height

        val displayMetrics = anchorView.resources.displayMetrics
        val screenW  = displayMetrics.widthPixels
        val screenH  = displayMetrics.heightPixels
        val margin   = (16 * displayMetrics.density).toInt()

        var posX = viewX + (viewW - popupWidthPx) / 2
        posX = posX.coerceIn(margin, screenW - popupWidthPx - margin)

        val posY = if (viewY - popupHeight - margin > margin) {
            viewY - popupHeight - (4 * displayMetrics.density).toInt()
        } else {
            viewY + viewH + (4 * displayMetrics.density).toInt()
        }

        show(rootView, posX, posY.coerceIn(margin, screenH - popupHeight - margin)) {
            contentView
        }
    }

    /** Fecha o popup ativo se houver. */
    fun dismiss() {
        activePopup?.dismiss()
        activePopup = null
    }

    /** True se um popup está visível. */
    val isVisible: Boolean get() = activePopup?.isShowing == true
}
