package task

import java.io.File
import java.io.InputStream
import java.util.LinkedList

const val EOF_SYMBOL = -1
const val ERROR_STATE = 0
const val SKIP_VALUE = 0

const val NEWLINE = '\n'.code

interface Automaton {
    val states: Set<Int>
    val alphabet: IntRange
    fun next(state: Int, symbol: Int): Int
    fun value(state: Int): Int
    val startState: Int
    val finalStates: Set<Int>
}

object Example : Automaton {
    override val states = setOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)
    override val alphabet = 0 .. 255
    override val startState = 1
    override val finalStates = setOf(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)

    private val numberOfStates = states.maxOrNull()!! + 1
    private val numberOfSymbols = alphabet.maxOrNull()!! + 1
    private val transitions = Array(numberOfStates) {IntArray(numberOfSymbols)}
    private val values: Array<Int> = Array(numberOfStates) {0}

    private fun setTransition(from: Int, symbol: Char, to: Int) {
        transitions[from][symbol.code] = to
    }

    private fun setValue(state: Int, terminal: Int) {
        values[state] = terminal
    }

    override fun next(state: Int, symbol: Int): Int =
        if (symbol == EOF_SYMBOL) ERROR_STATE
        else {
            assert(states.contains(state))
            assert(alphabet.contains(symbol))
            transitions[state][symbol]
        }

    override fun value(state: Int): Int {
        assert(states.contains(state))
        return values[state]
    }

    init {
        //Value float: [0-9]+(.[0-9]+)?
        //Start with x-krat [numbers], add [.], continue with y-krat [numbers]
        for(c in '0'..'9'){
            setTransition(1, c, 2)
        }
        for(c in '0'..'9'){
            setTransition(2, c, 2)
        }
        setTransition(2, '.', 3)
        for(c in '0'..'9'){
            setTransition(3, c, 4)
        }
        for(c in '0'..'9'){
            setTransition(4, c, 4)
        }
        //End with setValue (also checks if correct float is given)
        setValue(2,1)
        setValue(4,1)

        //Value variable: [a-zA-Z]+[0-9]*
        //Start with x-krat a-z/A-Z, y-krat [numbers] at the end are optional
        for(c in 'a'..'z'){
            setTransition(1,c,5)
        }
        for(c in 'A'..'Z'){
            setTransition(1,c,5)
        }
        for(c in 'a'..'z'){
            setTransition(5,c,5)
        }
        for(c in 'A'..'Z'){
            setTransition(5,c,5)
        }
        for(c in '0'..'9'){
            setTransition(5,c,6)
        }
        for(c in '0'..'9'){
            setTransition(6,c,6)
        }
        //End with setValue (also checks if correct value is given)
        setValue(5,2)
        setValue(6,2)

        //Symbol plus +
        setTransition(1,'+',7)
        setValue(7,3)

        //Symbol minus -
        setTransition(1,'-',8)
        setValue(8,4)

        //Symbol times *
        setTransition(1,'*',9)
        setValue(9,5)

        //Symbol divide /
        setTransition(1,'/',10)
        setValue(10,6)

        //Symbol pow ^
        setTransition(1,'^',11)
        setValue(11,7)

        //Symbol lparen (
        setTransition(1,'(',12)
        setValue(12,8)

        //Symbol rparen )
        setTransition(1,')',13)
        setValue(13,9)

        //Symbols to ignore
        setTransition(1,' ',14)
        setTransition(1,'\n',14)
        setTransition(1,'\t',14)
        setTransition(1,'\r',14)
        setValue(14,0)
    }
}

data class Token(val value: Int, val lexeme: String, val startRow: Int, val startColumn: Int)

class Scanner(private val automaton: Automaton, private val stream: InputStream) {
    private var state = automaton.startState
    private var last: Int? = null
    private var buffer = LinkedList<Byte>()
    private var row = 1
    private var column = 1

    private fun updatePosition(symbol: Int) {
        if (symbol == NEWLINE) {
            row += 1
            column = 1
        } else {
            column += 1
        }
    }

    private fun getValue(): Int {
        var symbol = last ?: stream.read()
        state = automaton.startState

        while (true) {
            updatePosition(symbol)

            val nextState = automaton.next(state, symbol)
            if (nextState == ERROR_STATE) {
                if (automaton.finalStates.contains(state)) {
                    last = symbol
                    return automaton.value(state)
                } else throw Error("Invalid pattern at ${row}:${column}")
            }
            state = nextState
            buffer.add(symbol.toByte())
            symbol = stream.read()
        }
    }

    fun eof(): Boolean =
        last == EOF_SYMBOL

    fun getToken(): Token? {
        if (eof()) return null

        val startRow = row
        val startColumn = column
        buffer.clear()

        val value = getValue()
        return if (value == SKIP_VALUE)
            getToken()
        else
            Token(value, String(buffer.toByteArray()), startRow, startColumn)
    }
}

fun name(value: Int) =
    when (value) {
        1 -> "float"
        2 -> "variable"
        3 -> "plus"
        4 -> "minus"
        5 -> "times"
        6 -> "divide"
        7 -> "pow"
        8 -> "lparen"
        9 -> "rparen"
        else -> throw Error("Invalid value")
    }

fun printTokens(scanner: Scanner) {
    val token = scanner.getToken()
    if (token != null) {
        print("${name(token.value)}(\"${token.lexeme}\") ")
        printTokens(scanner)
    }
}

//Run function
fun main(args: Array<String>) {
    val scanner = Scanner(Example, File(args[0]).inputStream())
    printTokens(scanner)
}


//Testing function
/*
fun main(args: Array<String>) {
    val scanner = Scanner(Example, "33.2 + - test123 44.023".byteInputStream())
    printTokens(scanner)
}
*/
