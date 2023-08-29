package parser

data class Point(
    val x: Float,
    val y: Float
)

sealed class SvgCommand {

    abstract val isAbsoluteCoordinates: Boolean

    data class MoveTo(
        override val isAbsoluteCoordinates: Boolean,
        val point: Point
    ) : SvgCommand() {

        companion object {
            const val pointsRequired = 1
        }

    }

    data class CubicBezierCurve(
        override val isAbsoluteCoordinates: Boolean,
        val point1: Point,
        val point2: Point,
        val point3: Point
    ) : SvgCommand() {

        companion object {
            const val pointsRequired = 3
        }

    }

    data class QuadraticBezierCurve(
        override val isAbsoluteCoordinates: Boolean,
        val point1: Point,
        val point2: Point
    ) : SvgCommand() {

        companion object {
            const val pointsRequired = 2
        }

    }

}

object SvgCommandParser {

    fun parse(pathData: String): List<SvgCommand> {
        val commandsRaw = pathData.split("(?=[A-z])".toRegex()).filter { it.isNotEmpty() }
        return commandsRaw.flatMap { parseCommand(it) }
    }

    private fun parseCommand(rawCommand: String): List<SvgCommand> {
        val commandCode = rawCommand[0]
        val numbers = rawCommand.substring(1)
            .split("[,\\s]".toRegex())
            .flatMap { it.split("(?=-)".toRegex()) }
            .filter { it.isNotEmpty() }
            .map { it.toFloat() }

        return when (commandCode) {
            'M', 'm' -> {
                numbers.chunked(SvgCommand.MoveTo.pointsRequired * 2).map {
                    SvgCommand.MoveTo(
                        isAbsoluteCoordinates = commandCode.isUpperCase(),
                        point = Point(it[0], it[1])
                    )
                }

            }

            'C', 'c' -> {
                numbers.chunked(SvgCommand.CubicBezierCurve.pointsRequired * 2).map {
                    SvgCommand.CubicBezierCurve(
                        isAbsoluteCoordinates = commandCode.isUpperCase(),
                        point1 = Point(it[0], it[1]),
                        point2 = Point(it[2], it[3]),
                        point3 = Point(it[4], it[5])
                    )
                }
            }

            'S', 's' -> {
                numbers.chunked(SvgCommand.CubicBezierCurve.pointsRequired * 2).map {
                    SvgCommand.QuadraticBezierCurve(
                        isAbsoluteCoordinates = commandCode.isUpperCase(),
                        point1 = Point(it[0], it[1]),
                        point2 = Point(it[2], it[3]),
                    )
                }
            }

            else -> throw UnsupportedOperationException("Unknown command[$commandCode]")
        }
    }


}