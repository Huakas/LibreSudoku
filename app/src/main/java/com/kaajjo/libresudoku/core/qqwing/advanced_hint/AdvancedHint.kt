package com.kaajjo.libresudoku.core.qqwing.advanced_hint

import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.utils.SudokuUtils


/**
 * Implemented:
 * Naked single
 * Hidden single
 * Full House
 * Locked candidates (type 1)
 * TODO:
 * Hidden subsets (pair, triple, quadruple)
 * Naked subsets (pair, triple, quadruple)
 * Locked candidates (type 2)
 * Wings (X, XY, XYZ)
 * Swordfish, Jellyfish
 * Chains and loops
 */

/**
 * **Very experimental** provides a hint
 *
 * @property type type of the game
 * @property board current sudoku board
 * @property solvedBoard solved sudoku board
 * @property userNotes user's notes for sudoku board
 * @property settings settings for an advanced hint
 */
class AdvancedHint(
    val type: GameType,
    private val board: List<List<Cell>>,
    private val solvedBoard: List<List<Cell>>,
    private var userNotes: List<Note> = emptyList(),
    private var settings: AdvancedHintSettings = AdvancedHintSettings()
) {
    private var notes: List<Note> = emptyList()
    init {
        if (notes.isEmpty()) {
            notes = SudokuUtils().computeNotes(board, type)
        }
    }

    private val rows = getRows()
    private val columns = getColumns()
    private val boxes = getBoxes()

    fun getEasiestHint(): AdvancedHintData? {
        val hint: AdvancedHintData? = null

        // easy hints
        if (settings.checkWrongValue) checkForWrongValue()?.let { return it }
        if (settings.fullHouse) checkForFullHouse()?.let { return it }
        if (settings.hiddenSingle) checkForHiddenSingle(notes)?.let { return it }
        if (settings.nakedSingle) checkForNakedSingle(notes)?.let { return it }

        // hints using locked candidates
        if (settings.lockedCandidates) {
            val lockedNotes = getLockedCandidateNotes()
            if (settings.hiddenSingle) checkForHiddenSingle(lockedNotes)?.let { return it }
            if (settings.nakedSingle) checkForNakedSingle(lockedNotes)?.let { return it }
        }

        // from here, hints are only changing notes

        return hint
    }

    private fun checkForWrongValue(): AdvancedHintData? {
        for (i in board.indices) {
            for (j in board.indices) {
                if (board[i][j].value != 0 && board[i][j].value != solvedBoard[i][j].value) {
                    return AdvancedHintData(
                        titleRes = R.string.hint_wrong_value_title,
                        textResWithArg = Pair(
                            R.string.hint_wron_value_detail,
                            listOf(
                                board[i][j].value.toString(),
                                cellStringFormat(board[i][j])
                            )
                        ),
                        targetCell = board[i][j],
                        helpCells = emptyList()
                    )
                }
            }
        }
        return null
    }

    private fun checkForNakedSingle(notes: List<Note>): AdvancedHintData? {
        if (notes.isEmpty()) return null
        val singles = notes.groupBy { Pair(it.row, it.col) }
            .filter { it.value.size == 1 }
            .map { it.value }
            .firstOrNull()

        return if (!singles.isNullOrEmpty()) {
            val nakedSingle = singles.first()
            val cell = solvedBoard[nakedSingle.row][nakedSingle.col]
            return AdvancedHintData(
                titleRes = R.string.hint_naked_single_title,
                textResWithArg = Pair(
                    R.string.hint_naked_single_detail,
                    listOf(
                        cellStringFormat(cell),
                        cell.value.toString()
                    )
                ),
                targetCell = cell,
                helpCells = emptyList()
            )
        } else {
            null
        }
    }
    
    private fun checkForFullHouse(): AdvancedHintData? {
        val entities = listOf(boxes, rows, columns)

        for (entity in entities) {
            val hint = checkEntityForFullHouse(entity)
            if (hint != null) {
                return hint
            }
        }

        return null
    }

    private fun checkEntityForFullHouse(entity: List<List<Cell>>): AdvancedHintData? {
        for (group in entity) {
            if (group.count { it.value != 0 } == type.size - 1) {
                val emptyCell = group.find { it.value == 0 }
                if (emptyCell != null) {
                    val solvedCell = solvedBoard[emptyCell.row][emptyCell.col]
                    return AdvancedHintData(
                        titleRes = R.string.hint_full_house_group_title,
                        textResWithArg = Pair(
                            R.string.hint_full_house_group_detail,
                            listOf(
                                cellStringFormat(emptyCell),
                                solvedCell.value.toString()
                            )
                        ),
                        targetCell = solvedBoard[emptyCell.row][emptyCell.col],
                        helpCells = board.flatten().filter { group.contains(it) }
                    )
                }
            }
        }
        return null
    }

    private fun checkForHiddenSingle(notes: List<Note>): AdvancedHintData? {
        if (notes.isEmpty()) return null
        val notesBoxes = getNotesBoxes()

        val singlesInBoxes = mutableListOf<Note>()
        for (box in notesBoxes) {
            val singlesInBox = box.groupBy { it.value }
                .filter { it.value.size == 1 }
                .map { it.value }
                .firstOrNull()
            if (singlesInBox?.firstOrNull() != null)
                singlesInBoxes.addAll(singlesInBox)
        }
        singlesInBoxes.sortBy { it.value }
        val singlesInRow = notes.groupBy { Pair(it.row, it.value) }
            .filter { it.value.size == 1 }
            .map { it.value }
            .minByOrNull { it[0].value }
        val singlesInColumn = notes.groupBy { Pair(it.col, it.value) }
            .filter { it.value.size == 1 }
            .map { it.value }
            .minByOrNull { it[0].value }

        val hiddenSingle: Note?
        val hintDetail: Int

        if (singlesInBoxes.firstOrNull() != null) {
            hiddenSingle = singlesInBoxes.first()
            hintDetail = R.string.hint_hidden_single_group_detail
        } else if (singlesInRow?.firstOrNull() != null) {
            hiddenSingle = singlesInRow.first()
            hintDetail = R.string.hint_hidden_single_row_detail
        } else if (singlesInColumn?.firstOrNull() != null) {
            hiddenSingle = singlesInColumn.first()
            hintDetail = R.string.hint_hidden_single_column_detail
        } else
            return null

        val cell = solvedBoard[hiddenSingle.row][hiddenSingle.col]
        return AdvancedHintData(
            titleRes = R.string.hint_hidden_single_title,
            textResWithArg = Pair(
                hintDetail,
                listOf(
                    cellStringFormat(cell),
                    cell.value.toString()
                )
            ),
            targetCell = cell,
            helpCells = emptyList()
        )
    }

    private fun getRows(): List<List<Cell>> {
        return board
    }

    private fun getColumns(): List<List<Cell>> {
        val transposedBoard =
            MutableList(type.size) { row -> MutableList(type.size) { col -> Cell(row, col, 0) } }

        for (i in 0 until type.size) {
            for (j in 0 until type.size) {
                transposedBoard[j][i] = board[i][j]
            }
        }
        return transposedBoard.toList()
    }

    private fun getBoxes(): List<List<Cell>> {
        val size = type.size
        val sectionWidth = type.sectionWidth
        val sectionHeight = type.sectionHeight

        val boxes = MutableList(sectionWidth * sectionHeight) { mutableListOf<Cell>() }
        for (i in 0 until size) {
            for (j in 0 until size) {
                val sectionRow = i / sectionHeight
                val sectionColumn = j / sectionWidth
                val sectorsPerRow = size / sectionWidth
                val boxNumber = sectionRow * sectorsPerRow + sectionColumn
                boxes[boxNumber].add(board[i][j])
            }
        }
        return boxes
    }

    private fun getNotesBoxes(): List<List<Note>> {
        val sectionWidth = type.sectionWidth
        val sectionHeight = type.sectionHeight

        val notesBoxes = MutableList(sectionWidth * sectionHeight) { mutableListOf<Note>() }
        for (i in 0 until sectionWidth * sectionHeight) {
            notesBoxes[i].addAll(notes.filter {
                it.row / sectionHeight == i / sectionHeight &&
                it.col / sectionWidth == i % sectionHeight
            })
        }
        return notesBoxes
    }

    private fun getLockedCandidates1(): Pair<List<List<Note>>, List<List<Note>>> {
        val returnObject = Pair(mutableListOf<List<Note>>(), mutableListOf<List<Note>>())
        val notesBoxes = getNotesBoxes()
        notesBoxes.forEach { element ->
            val candidatesInRow = element.groupBy { it.value }
                .filter { it.value.groupBy { it2 -> it2.row }.size == 1 }
                .map { it.value }
            val candidatesInColumn = element.groupBy { it.value }
                .filter { it.value.groupBy { it2 -> it2.col }.size == 1 }
                .map { it.value }

            returnObject.first.addAll(candidatesInRow)
            returnObject.second.addAll(candidatesInColumn)
        }
        return returnObject
    }

    private fun getLockedCandidates2(): Pair<List<List<Note>>, List<List<Note>>> {
        val sectionWidth = type.sectionWidth
        val sectionHeight = type.sectionHeight

        val returnObject = Pair(mutableListOf<List<Note>>(), mutableListOf<List<Note>>())
        val newNotes = notes.toMutableList()
        newNotes.groupBy { it.row }
            .forEach { rowItem ->
                val candidatesInRow = rowItem.value.groupBy { it.value }
                    .filter { it.value.groupBy { it2 -> it2.col / sectionWidth }.size == 1 }
                    .map { it.value }

                returnObject.first.addAll(candidatesInRow)
            }
        newNotes.groupBy { it.col }
            .forEach { colItem ->
                val candidatesInCol = colItem.value.groupBy { it.value }
                    .filter { it.value.groupBy { it2 -> it2.row / sectionHeight }.size == 1 }
                    .map { it.value }

                returnObject.first.addAll(candidatesInCol)
            }
        return returnObject
    }

    private fun getLockedCandidateNotes(): List<Note> {
        val newNotes = notes.toMutableList()

        // locked candidates type 1
        val candidates1 = getLockedCandidates1()
        candidates1.first.forEach { row ->
            row.forEach { candidate ->
                val currentRow = candidate.row
                val currentValue = candidate.value
                newNotes.removeIf {
                    it.row == currentRow &&
                    it.value == currentValue &&
                    it !in row
                }
            }
        }
        candidates1.second.forEach { col ->
            col.forEach { candidate ->
                val currentCol = candidate.col
                val currentValue = candidate.value
                newNotes.removeIf {
                    it.col == currentCol &&
                    it.value == currentValue &&
                    it !in col
                }
            }
        }

        return newNotes
    }

    private fun cellStringFormat(cell: Cell) = "r${cell.row + 1}c${cell.col + 1}"
}