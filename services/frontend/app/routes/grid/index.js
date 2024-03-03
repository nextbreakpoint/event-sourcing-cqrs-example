let makeRandom = function() {
    let date = new Date()
    date.setHours(0, 0, 0, 0)
	let seed = date.getTime()
	return function() {
		seed = ((seed + 0x7ED55D16) + (seed <<  12)) & 0xFFFFFFFF
		seed = ((seed ^ 0xC761C23C) ^ (seed >>> 19)) & 0xFFFFFFFF
		seed = ((seed + 0x165667B1) + (seed <<   5)) & 0xFFFFFFFF
		seed = ((seed + 0xD3A2646C) ^ (seed <<   9)) & 0xFFFFFFFF
		seed = ((seed + 0xFD7046C5) + (seed <<   3)) & 0xFFFFFFFF
		seed = ((seed ^ 0xB55A4F09) ^ (seed >>> 16)) & 0xFFFFFFFF
		return (seed & 0xFFFFFFF) / 0x10000000
	}
}

let pattern1 = [
    [ [0,3,0,0], [0,3,0,1], [0,3,0,2], [0,3,0,3], [0,3,0,4], [0,3,0,5], [0,3,0,6], [0,3,0,7] ],
    [ [0,3,1,0], [0,3,1,1], [0,3,1,2], [0,3,1,3], [0,3,1,4], [0,3,1,5], [0,3,1,6], [0,3,1,7] ],
    [ [0,3,2,0], [0,3,2,1], [0,3,2,2], [0,3,2,3], [0,3,2,4], [0,3,2,5], [0,3,2,6], [0,3,2,7] ],
    [ [0,3,3,0], [0,3,3,1], [0,3,3,2], [0,3,3,3], [0,3,3,4], [0,3,3,5], [0,3,3,6], [0,3,3,7] ],
    [ [0,3,4,0], [0,3,4,1], [0,3,4,2], [0,3,4,3], [0,3,4,4], [0,3,4,5], [0,3,4,6], [0,3,4,7] ],
    [ [0,3,5,0], [0,3,5,1], [0,3,5,2], [0,3,5,3], [0,3,5,4], [0,3,5,5], [0,3,5,6], [0,3,5,7] ],
    [ [0,3,6,0], [0,3,6,1], [0,3,6,2], [0,3,6,3], [0,3,6,4], [0,3,6,5], [0,3,6,6], [0,3,6,7] ],
    [ [0,3,7,0], [0,3,7,1], [0,3,7,2], [0,3,7,3], [0,3,7,4], [0,3,7,5], [0,3,7,6], [0,3,7,7] ]
]

let pattern1a = [
    [ [0,3,2,0], [0,3,2,1], [0,3,2,2], [0,3,2,3], [0,3,2,4], [0,3,2,5], [0,3,2,6], [0,3,2,7] ],
    [ [0,3,3,0], [0,3,3,1], [0,3,3,2], [0,3,3,3], [0,3,3,4], [0,3,3,5], [0,3,3,6], [0,3,3,7] ],
    [ [0,3,4,0], [0,3,4,1], [0,3,4,2], [0,3,4,3], [0,3,4,4], [0,3,4,5], [0,3,4,6], [0,3,4,7] ],
    [ [0,3,5,0], [0,3,5,1], [0,3,5,2], [0,3,5,3], [0,3,5,4], [0,3,5,5], [0,3,5,6], [0,3,5,7] ]
]

let pattern2a = [
    [ [0,2,0,0], [0,2,0,1], [0,2,0,2], [0,2,0,3], [1,2,0,0], [1,2,0,1], [1,2,0,2], [1,2,0,3] ],
    [ [0,2,1,0], [0,2,1,1], [0,2,1,2], [0,2,1,3], [1,2,1,0], [1,2,1,1], [1,2,1,2], [1,2,1,3] ],
    [ [0,2,2,0], [0,2,2,1], [0,2,2,2], [0,2,2,3], [1,2,2,0], [1,2,2,1], [1,2,2,2], [1,2,2,3] ],
    [ [0,2,3,0], [0,2,3,1], [0,2,3,2], [0,2,3,3], [1,2,3,0], [1,2,3,1], [1,2,3,2], [1,2,3,3] ]
]

let pattern4a = [
    [ [0,2,0,1], [0,2,0,2], [1,2,0,1], [1,2,0,2], [2,2,0,1], [2,2,0,2], [3,2,0,1], [3,2,0,2] ],
    [ [0,2,1,1], [0,2,1,2], [1,2,1,1], [1,2,1,2], [2,2,1,1], [2,2,1,2], [3,2,1,1], [3,2,1,2] ],
    [ [0,2,2,1], [0,2,2,2], [1,2,2,1], [1,2,2,2], [2,2,2,1], [2,2,2,2], [3,2,2,1], [3,2,2,2] ],
    [ [0,2,3,1], [0,2,3,2], [1,2,3,1], [1,2,3,2], [2,2,3,1], [2,2,3,2], [3,2,3,1], [3,2,3,2] ]
]

let pattern5a = [
    [ [0,2,0,0], [0,2,0,1], [0,2,0,2], [0,2,0,3], [1,1,0,0], [1,1,0,1], [2,1,0,0], [2,1,0,1] ],
    [ [0,2,1,0], [0,2,1,1], [0,2,1,2], [0,2,1,3], [1,1,1,0], [1,1,1,1], [2,1,1,0], [2,1,1,1] ],
    [ [0,2,2,0], [0,2,2,1], [0,2,2,2], [0,2,2,3], [3,1,0,0], [3,1,0,1], [4,1,0,0], [4,1,0,1] ],
    [ [0,2,3,0], [0,2,3,1], [0,2,3,2], [0,2,3,3], [3,1,1,0], [3,1,1,1], [4,1,1,0], [4,1,1,1] ]
]

let pattern5b = [
    [ [0,1,0,0], [0,1,0,1], [1,1,0,0], [1,1,0,1], [4,2,0,0], [4,2,0,1], [4,2,0,2], [4,2,0,3] ],
    [ [0,1,1,0], [0,1,1,1], [1,1,1,0], [1,1,1,1], [4,2,1,0], [4,2,1,1], [4,2,1,2], [4,2,1,3] ],
    [ [2,1,0,0], [2,1,0,1], [3,1,0,0], [3,1,0,1], [4,2,2,0], [4,2,2,1], [4,2,2,2], [4,2,2,3] ],
    [ [2,1,1,0], [2,1,1,1], [3,1,1,0], [3,1,1,1], [4,2,3,0], [4,2,3,1], [4,2,3,2], [4,2,3,3] ]
]

let pattern5c = [
    [ [0,1,0,0], [0,1,0,1], [4,2,0,0], [4,2,0,1], [4,2,0,2], [4,2,0,3], [1,1,0,0], [1,1,0,1] ],
    [ [0,1,1,0], [0,1,1,1], [4,2,1,0], [4,2,1,1], [4,2,1,2], [4,2,1,3], [1,1,1,0], [1,1,1,1] ],
    [ [2,1,0,0], [2,1,0,1], [4,2,2,0], [4,2,2,1], [4,2,2,2], [4,2,2,3], [3,1,0,0], [3,1,0,1] ],
    [ [2,1,1,0], [2,1,1,1], [4,2,3,0], [4,2,3,1], [4,2,3,2], [4,2,3,3], [3,1,1,0], [3,1,1,1] ]
]

let addOffset = function(pattern, offset) {
     return pattern.map((row) => {
        return row.map((col) => {
             let copy = col.map((x) => x)
             copy[0] = col[0] + offset
             return copy
        })
     })
}

let maxOffset = function(pattern) {
    return pattern.reduce((max, row) => {
        return row.reduce((max, col) => {
            return Math.max(max, col[0])
        }, max)
    }, 0)
}

let mergePatterns = function(patterns) {
    let accumulator = patterns.reduce((accumulator, pattern) => {
        return {
            pattern: accumulator.pattern.concat(addOffset(pattern, accumulator.offset)),
            offset: accumulator.offset + maxOffset(pattern) + 1
        }
    }, {
        pattern: [],
        offset: 0
    })

    return accumulator.pattern
}

let patterns = [
    mergePatterns([ pattern1a, pattern5a, pattern2a, pattern2a ]),
    mergePatterns([ pattern5b, pattern2a, pattern1a, pattern2a ]),
    mergePatterns([ pattern2a, pattern5c, pattern2a, pattern1a ]),
    mergePatterns([ pattern2a, pattern1a, pattern2a, pattern5b ]),
    mergePatterns([ pattern1a, pattern2a, pattern5c, pattern2a ]),
    mergePatterns([ pattern1a, pattern4a, pattern1a, pattern4a ]),
    mergePatterns([ pattern2a, pattern4a, pattern2a, pattern2a ])
]

function render(designs, pattern) {
    return pattern.map((row) => {
        return row.map((col) => {
            let design = designs[col[0] % designs.length]
            let url = design.baseUrl + "/" + design.uuid + "/" + col[1] + "/" + col[3] + "/" + col[2] + "/256.png?t=" + design.checksum
            let cell = {
                design: design,
                imageUrl: url
            }
            return cell
        })
    })
}

function Instance() {
    var self = this
}

Instance.prototype.make = function (designs, from, size, rows) {
    var random = makeRandom()

    if (rows == 8 && designs.length == 1) {
        return render(designs, pattern1)
    }

    if (rows == 16 && designs.length > 0) {
        let patternIdx = (Math.round(random() * (patterns.length - 1)) + from / size) % patterns.length

        let pattern = patterns[patternIdx]

        return render(designs, pattern)
    }

    return []
}

module.exports = new Instance()
