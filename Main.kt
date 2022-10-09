package calculator
import java.util.ArrayDeque
import java.math.BigInteger

const val PS_OK = 0
const val PS_COMMAND_INVALID = 1
const val PS_EXPR_INVALID = 2
const val PS_ID_INVALID = 3
const val PS_ASSIGNMENT_INVALID = 4
const val PS_COMMAND = 5
const val PS_ASSIGNMENT = 6
const val PS_EXIT = 7
const val PS_CONTINUE = 8
const val PS_UNKNOWN_VAR = 9

fun errorHandle(status: Int): Int {
    val errors = listOf(PS_ASSIGNMENT_INVALID, PS_ID_INVALID, PS_EXPR_INVALID, PS_COMMAND_INVALID, PS_UNKNOWN_VAR)
    val errType =
        when (status) {
            PS_UNKNOWN_VAR -> "variable"
            PS_EXPR_INVALID -> "expression"
            PS_COMMAND_INVALID -> "command"
            PS_ID_INVALID -> "identifier"
            PS_ASSIGNMENT_INVALID -> "assignment"
            else -> ""
        }
    if (status in errors) {
        println(
            if (status == PS_COMMAND_INVALID || status == PS_UNKNOWN_VAR) {
                "Unknown "
            } else {
                "Invalid "
            } + errType
        )
        return PS_CONTINUE
    }
    return if (status == PS_CONTINUE) status else PS_OK
}

fun printID(inp: String, variables: MutableMap<String, BigInteger>): Int {

    if (inp.toBigIntegerOrNull() != null) {
        println(inp.toBigInteger())
    } else {
        if (variables.containsKey(inp)) {
            println(variables[inp])
        } else {
            return PS_UNKNOWN_VAR
        }
    }
    return PS_OK
}

fun main() {
    val variables = mutableMapOf<String, BigInteger>()
    while (true) {
        var inp = readln()
        if (inp == "") continue

        inp = inp.replace(Regex("\\s+"), "")
        inp = removeRedundantOps(inp)
        val checkInputStatus = checkInput(inp, variables)
        if (errorHandle(checkInputStatus) == PS_CONTINUE) continue

        if (checkInputStatus == PS_ASSIGNMENT) {
            assignVars(variables, inp)
            continue
        }
        when (val mAI = multipleArgsInput(inp, variables)) {
            PS_EXIT -> return
            PS_EXPR_INVALID, PS_ID_INVALID -> {
                errorHandle(mAI)
                continue
            }
        }
    }
}

fun assignVars(variables: MutableMap<String, BigInteger>, inp: String) {
    val separated = inp.split("=")
    if (separated[1].trim().toBigIntegerOrNull() == null) {
        if (!variables.containsKey(separated[1].trim())) println("Unknown variable")
        else variables[separated[0].trim()] = variables.getValue(separated[1].trim())
    } else {
        variables[separated[0].trim()] = separated[1].trim().toBigInteger()
    }
}

fun checkInput(inp: String, variables: MutableMap<String, BigInteger>): Int {
    val validCommand = Regex("/.*")
    val knownCommands = Regex("/(exit|help)")
    val singleNumOrVar = Regex("[+-]?(\\d+|[a-zA-Z]+)")
    val correctExpression =
        Regex("(\\(*[+-]?\\(*(\\d+|[a-zA-Z]+)\\)*(\\++|-+|\\*|/|\\^)[()]*)+(\\d+|[a-zA-Z]+)\\)*")
    val isAssignment = Regex(".*=.*") // anything that has at least one '='

    if (inp.matches(validCommand)) {
        return if (!inp.matches(knownCommands)) PS_COMMAND_INVALID else PS_COMMAND
    }
    if (inp.matches(isAssignment)) return handleAssignment(inp)

    if (inp.all { it.isLetter() } || inp.substring(1, inp.length - 1).all { it.isDigit() }) {
        return if (!inp.matches(singleNumOrVar)) {
            PS_ID_INVALID
        } else {
            if (printID(inp, variables) == PS_UNKNOWN_VAR) {
                PS_UNKNOWN_VAR
            } else {
                PS_CONTINUE
            }
        }
    }

    if (!inp.matches(correctExpression)) return PS_EXPR_INVALID
    return PS_OK
}

fun handleAssignment(inp: String): Int {
    val assignment = Regex("[a-zA-Z]+=([a-zA-Z]+|-?\\d+)")
    val validID = Regex("[a-zA-Z]+=.*")
    return if (inp.matches(assignment)) {
        PS_ASSIGNMENT
    } else {
        if (!inp.matches(validID)) PS_ID_INVALID
        else PS_ASSIGNMENT_INVALID
    }
}

fun transformToPostfix(inp: String): String? {
    val stack = ArrayDeque<Char>()
    var result = ""
    val operandPriorities = mapOf('+' to 1, '-' to 1, '*' to 2, '/' to 2)
    for (op in inp) {
        if (op.isDigit() || op.isLetter()) {
            result += "$op"
        } else if (stack.isEmpty() || stack.peek() == '(' || op == '(') {
            result += " "
            stack.push(op)
        } else if (op == ')') {
            if (stack.isEmpty()) return null
            var item = stack.pop()
            while (item != '(') {
                if (stack.isEmpty()) return null
                result += " $item "
                item = stack.pop()
            }
        } else if ((operandPriorities[op]!! > operandPriorities[stack.peek()]!!)) {
            result += " "
            stack.push(op)
        }  else {
            while (true) {
                if (stack.isEmpty() || stack.peek() == '(' ||
                    operandPriorities[stack.peek()]!! < operandPriorities[op]!!) {
                    stack.push(op)
                    break
                } else {
                    result += " " + stack.pop() + " "
                }
            }
        }
    }
    while (!stack.isEmpty()) {
        val item = stack.pop()
        if (item == '(' || item == ')') {
            return null
        }
        result += " $item"
    }
    return result
}

fun calculateResult(inp: String, variables: MutableMap<String, BigInteger>): BigInteger? {
    val stack = ArrayDeque<BigInteger>()
    for (elem in inp.split(' ')) {
        if (elem == "") continue
        if (elem.toBigIntegerOrNull() != null) {
            stack.push(elem.toBigInteger())
        } else if (variables.containsKey(elem)) {
            stack.push(variables[elem]!!)
        } else if (mutableListOf("+", "-", "/", "*").contains(elem)) {
            val a = stack.pop()
            val b = stack.pop()
            stack.push(performOperation(a, b, elem))
        } else {
            return null
        }
    }
    return stack.pop()
}

fun performOperation(a: BigInteger, b: BigInteger, e: String): BigInteger {
    return when (e) {
        "+" -> b + a
        "-" -> b - a
        "*" -> b * a
        "/" -> b / a
        else -> 0.toBigInteger()
    }
}

fun multipleArgsInput(inp: String, variables: MutableMap<String, BigInteger>): Int {
    when (inp) {
        "/exit" -> {
            println("Bye!")
            return PS_EXIT
        }
        "/help" -> printHelp()
    }

    val inpFormatted = transformToPostfix(inp) ?: return PS_EXPR_INVALID
    val result = calculateResult(inpFormatted, variables) ?: return PS_ID_INVALID

    println(result)
    return PS_OK
}

fun removeRedundantOps(inp: String): String {
    var out: String = inp.replace(Regex("--"), "+")
    out = out.replace(Regex("\\++"), "+")
    out = out.replace(Regex("(-\\+|\\+-)"), "-")
    return out
}

fun printHelp() {
    println("""
The program handles basic calculator operations.

COMMANDS
    /help
        prints this help
    /exit
        ends the program

Start typing any basic math operation composed of [SUPPORTED OPERATORS].
You can also declare variables with '=', i.e. MY_VAR = 50
and later you can use them in your calculations or display their value by typing their name.

Hit ENTER to launch the calculator and get your result!

SUPPORTED OPERATORS
    +   binary addition
    -   unary and binary subtraction
    *   binary multiplication
    /   binary division
    ()  parentheses

EXAMPLES ('>' represents user input):
> a = 5
> b = 3
> b
3
> (a + 5) * b / 3 + (4*5+b)
33
> /exit
Bye!
"""
    )
}
