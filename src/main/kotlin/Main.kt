package calculator
import java.math.BigInteger
private const val INFO = "The program can solve and save expressions with integers numbers and + - * / ^ operators.\n" +
		"  Saving an expression   ->    identifier = expression [ example: x = (5 * (-2 + -6))  ]\n" +
		"  Saving a value         ->    identifier = value      [ example: y = 4   or   y = x   ]\n" +
		"  Evaluate an expression ->    expression              [ example: 6 ^ 2 * -(-(20 / y)) ]\n" +
		"Spaces are ignored, write /help for info, /exit for terminate the program."

fun main() {
	var variablesMap = mutableMapOf<String, String>()
	while (true) {
		variablesMap = shortMapByKeyLength(variablesMap)
		val input = readln()
		when {
			input == "check variables" -> println(variablesMap)
			input == "" -> continue
			input == "/help" -> println(INFO)
			input == "/exit" -> break
			input[0] == '/' -> println("Unknown command")
			"[+-]".toRegex().matches(input.last().toString()) -> println("Unknown command here")
			input.split("=").size == 2 ->
				try {
					val keyValue = input.split("=")
					variablesMap = assignVariable(variablesMap, keyValue[0], keyValue[1])
				} catch (e:Exception) {
					println(e.message)
				}
			input.split("=").size > 2 -> println("Invalid assignment")
			else -> {
				try {
					val changedString = input.changeVariables(variablesMap)
					val postfix = infixToPostfix(changedString)
					println(postfix.calculate())
				} catch (e: Exception) {
					println(e.message)
				}
			}
		}
	}
	println("Bye!")
}
private fun shortMapByKeyLength(variablesMap:MutableMap<String, String>):MutableMap<String, String> {
	val resultMap = emptyMap<String, String>().toMutableMap()
	val selector: (String) -> Int = { str -> str.length }
	val helpfulArray = variablesMap.keys.sortedByDescending(selector)
	for (elem in helpfulArray){
		val key = variablesMap[elem]
		resultMap += mapOf(elem to key!!)
	}
	return resultMap
}

fun infixToPostfix(input: String): String {

	var infix = input.replace(" ","")
	val invalidRegex = """[*/^]{2,}""".toRegex()
	if (infix.contains(invalidRegex)) throw  InvalidIdentifier()
	val plusOrMinusRegex = Regex("([+-]){2,}")
	while (infix.contains(plusOrMinusRegex)) {
		infix = infix.replace("--","+")
			.replace("++","+")
			.replace("+-","-")
			.replace("-+","-")
	}
	infix = if (infix.trim()[0] == '-' || infix.trim()[0] == '+') "0 $infix " else "$infix "
	var utilToInfixString = ""
	var openCount = 0
	var closeCount = 0
	for ((i,char) in infix.withIndex()) {
		if (char == ' ') continue
		if (char == '(') openCount++
		if (char == ')') closeCount++
		if (char.isDigit() && infix[i + 1].isDigit()) utilToInfixString += char.toString() else utilToInfixString += "$char "
	}
	if (openCount != closeCount) throw InvalidIdentifier()
	infix = utilToInfixString

	var postfix = ""
	val operatorStack: ArrayDeque<String> = ArrayDeque()
	val operatorsValue =
		mapOf(
			"(" to -1,
			"+" to 0,
			"-" to 0,
			"*" to 1,
			"/" to 1,
			"^" to 2
		)
	outer@for ((i, char) in infix.withIndex()) {
		when {
			char == ' ' -> continue
			char.isDigit() -> {
				postfix += char.toString()
				if (!infix[i + 1].isDigit()) {
					postfix += " "
				}
			}
			char == '(' -> operatorStack.add("$char")
			char == ')' -> {
				var count = operatorStack.size - 1
				while (operatorStack[count] != "(") {
					postfix += "${operatorStack[count]} "
					operatorStack.removeAt(count)
					count--
				}
				operatorStack.removeAt(count)
			}
			operatorsValue.containsKey(char.toString())-> {
				if (operatorStack.isEmpty()) {
					operatorStack.add(char.toString())
				} else {
					for (ind in operatorStack.size - 1 downTo  0) {
						val newOperator = char.toString()
						val newOperatorValue = operatorsValue.getValue(newOperator)
						var queueOperator = operatorStack[ind]
						var queueOperatorValue = operatorsValue.getValue(operatorStack[ind])
						if (newOperatorValue <= queueOperatorValue) {
							var count = ind
							while (newOperatorValue <= queueOperatorValue) {
								postfix += "$queueOperator "
								operatorStack.removeAt(count)
								if (!operatorStack.isEmpty()) {
									queueOperator = operatorStack[count - 1]
									queueOperatorValue = operatorsValue.getValue(operatorStack[count - 1])
									count--
								} else {
									operatorStack.add(newOperator)
									continue@outer
								}
							}
							operatorStack.add(newOperator)
							continue@outer
						} else {
							operatorStack.add(newOperator)
							continue@outer
						}
					}
				}
			}
		}
	}
	for (i in operatorStack.size - 1 downTo 0) {
		postfix += "${ operatorStack[i] } "
	}
	return postfix
}

private fun String.calculate(): BigInteger {
	val numberStack: ArrayDeque<String> = ArrayDeque()
	val elemArray = this.trim().split(" ").toMutableList()
	for (elem in elemArray) {
		if (elem.toBigIntegerOrNull() != null) {
			numberStack.add(elem)
		} else {
			var result = BigInteger.ZERO
			val numberStackLast = numberStack.last().toBigInteger()
			val numberStackBeforeLast = numberStack[numberStack.lastIndex - 1].toBigInteger()
			when (elem)  {
				"+" -> { result = numberStackBeforeLast + numberStackLast }
				"-" -> { result = numberStackBeforeLast - numberStackLast }
				"/" -> { result = numberStackBeforeLast / numberStackLast }
				"*" -> { result = numberStackBeforeLast * numberStackLast }
				"^" -> { result = numberStackBeforeLast.pow(numberStackLast.toInt()) }
			}
			numberStack.removeAt(numberStack.lastIndex)
			numberStack.removeAt(numberStack.lastIndex)
			numberStack.add(result.toString())
		}
	}
	return(numberStack.last().toBigInteger())
}


class InvalidIdentifier: Exception("Invalid Identifier")
class UnknownVariable: Exception("Unknown variable")
class InvalidAssignment: java.lang.Exception("Invalid assignment")

private fun assignVariable(variablesMap:MutableMap<String, String>, key: String, value: String):MutableMap<String, String> {
	val keyRegex = """[A-z]*""".toRegex()
	if (!key.trim().matches(keyRegex)){
		throw InvalidIdentifier()
	}
	var newValue = value.changeVariables(variablesMap)
	newValue = newValue.updateVariable(variablesMap)
	if (!newValue.testInput()) throw InvalidAssignment()
	newValue = infixToPostfix(newValue)
	newValue = newValue.calculate().toString()
	variablesMap += mapOf(key.trim() to newValue)
	return variablesMap
}

private fun String.updateVariable(variablesMap:MutableMap<String, String>):String {
	var updatedString = this
	for (variable in variablesMap) {
		if (updatedString.matches(variable.key.toRegex())) {
			updatedString = (updatedString.replace(variable.key.toRegex(), variable.value))
		}
	}
	return updatedString
}

private fun String.changeVariables(variablesMap:MutableMap<String, String>): String {
	var changedString = this
	for(variable in variablesMap) {
		changedString = changedString.replace(variable.key, variable.value)
	}
	val keyRegex = """[A-z]+""".toRegex()
	if (changedString.matches(keyRegex)) throw UnknownVariable()
	return changedString
}

private fun String.testInput():Boolean {
	val myRegex = """[+\d */(^)-]*""".toRegex()
	return this.matches(myRegex)
}