export default function computePercentage(design, levels) {
    const total = levels.map(i => design.tiles[i].total)
        .reduce((previousValue, currentValue) => previousValue + currentValue, 0)

    const completed = levels.map(i => design.tiles[i].completed)
        .reduce((previousValue, currentValue) => previousValue + currentValue, 0)

    const percentage = Math.floor((completed * 100.0) / total)

    return percentage
}
