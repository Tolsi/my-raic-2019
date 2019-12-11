import korma_geom.Point

enum class Direction (var point: Point) {
    Top(Point(0, 1)),
    Down(Point(0, -1)),
    Left(Point(-1, 0)),
    Right(Point(1, 0))
}

enum class VerticalDirection (var point: Point) {
    Top(Point(0, 1)),
    Down(Point(0, -1)),
}

enum class HorizontalDirection (var point: Point) {
    Left(Point(-1, 0)),
    Right(Point(1, 0))
}
