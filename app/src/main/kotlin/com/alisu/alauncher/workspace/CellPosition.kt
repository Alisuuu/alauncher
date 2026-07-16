package com.alisu.alauncher.workspace

/**
 * Representa a posição de um item na grade do Workspace.
 *
 * @param cellX    Coluna (0-indexed)
 * @param cellY    Linha (0-indexed)
 * @param spanX    Largura em células (padrão 1 — apps normais)
 * @param spanY    Altura em células (padrão 1 — apps normais)
 * @param screenId Índice da página no ViewPager2 (padrão 0)
 */
data class CellPosition(
    val cellX: Int,
    val cellY: Int,
    val spanX: Int    = 1,
    val spanY: Int    = 1,
    val screenId: Int = 0
) {
    companion object {

        /**
         * Converte coordenadas de pixel absolutas (rawX, rawY) para uma CellPosition.
         * Retorna null se as coordenadas estiverem fora dos limites da grade.
         *
         * @param rawX       Coordenada X absoluta na tela
         * @param rawY       Coordenada Y absoluta na tela
         * @param rvLeft     Posição esquerda do RecyclerView na tela
         * @param rvTop      Posição topo do RecyclerView na tela
         * @param cellWidth  Largura de cada célula em pixels
         * @param cellHeight Altura de cada célula em pixels
         * @param columns    Número de colunas da grade
         * @param rows       Número de linhas da grade
         */
        fun fromPixel(
            rawX: Float,
            rawY: Float,
            rvLeft: Int,
            rvTop: Int,
            cellWidth: Int,
            cellHeight: Int,
            columns: Int,
            rows: Int
        ): CellPosition? {
            if (cellWidth <= 0 || cellHeight <= 0) return null
            val col = ((rawX - rvLeft) / cellWidth).toInt()
            val row = ((rawY - rvTop) / cellHeight).toInt()
            if (col < 0 || col >= columns || row < 0 || row >= rows) return null
            return CellPosition(cellX = col, cellY = row)
        }
    }

    /** Índice linear na lista flat (row-major). */
    fun toLinearIndex(columns: Int): Int = cellY * columns + cellX

    /** Verifica se esta posição é válida dentro dos limites dados. */
    fun isValid(columns: Int, rows: Int): Boolean =
        cellX >= 0 && cellX < columns && cellY >= 0 && cellY < rows
}
