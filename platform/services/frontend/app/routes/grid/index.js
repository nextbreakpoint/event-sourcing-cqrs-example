function Instance() {
    var self = this
}

let makeRandom = function() {
    var date = new Date()
    date.setHours(0, 0, 0, 0)
	var seed = date.getTime() //original seed 0x2F6E2B1
	return function() {
		// Robert Jenkins’ 32 bit integer hash function
		seed = ((seed + 0x7ED55D16) + (seed << 12))  & 0xFFFFFFFF
		seed = ((seed ^ 0xC761C23C) ^ (seed >>> 19)) & 0xFFFFFFFF
		seed = ((seed + 0x165667B1) + (seed << 5))   & 0xFFFFFFFF
		seed = ((seed + 0xD3A2646C) ^ (seed << 9))   & 0xFFFFFFFF
		seed = ((seed + 0xFD7046C5) + (seed << 3))   & 0xFFFFFFFF
		seed = ((seed ^ 0xB55A4F09) ^ (seed >>> 16)) & 0xFFFFFFFF
		return (seed & 0xFFFFFFF) / 0x10000000
	}
}

let pattern1 = [
    [ [0,3,2,0], [0,3,2,1], [0,3,2,2], [0,3,2,3], [0,3,2,4], [0,3,2,5], [0,3,2,6], [0,3,2,7] ],
    [ [0,3,3,0], [0,3,3,1], [0,3,3,2], [0,3,3,3], [0,3,3,4], [0,3,3,5], [0,3,3,6], [0,3,3,7] ],
    [ [0,3,4,0], [0,3,4,1], [0,3,4,2], [0,3,4,3], [0,3,4,4], [0,3,4,5], [0,3,4,6], [0,3,4,7] ],
    [ [0,3,5,0], [0,3,5,1], [0,3,5,2], [0,3,5,3], [0,3,5,4], [0,3,5,5], [0,3,5,6], [0,3,5,7] ]
]

let pattern2 = [
    [ [0,2,0,0], [0,2,0,1], [0,2,0,2], [0,2,0,3], [1,2,0,0], [1,2,0,1], [1,2,0,2], [1,2,0,3] ],
    [ [0,2,1,0], [0,2,1,1], [0,2,1,2], [0,2,1,3], [1,2,1,0], [1,2,1,1], [1,2,1,2], [1,2,1,3] ],
    [ [0,2,2,0], [0,2,2,1], [0,2,2,2], [0,2,2,3], [1,2,2,0], [1,2,2,1], [1,2,2,2], [1,2,2,3] ],
    [ [0,2,3,0], [0,2,3,1], [0,2,3,2], [0,2,3,3], [1,2,3,0], [1,2,3,1], [1,2,3,2], [1,2,3,3] ]
]

let pattern3 = [
    [ [0,1,0,0], [0,1,0,1], [1,1,0,0], [1,1,0,1], [2,1,0,0], [2,1,0,1], [3,1,0,0], [3,1,0,1] ],
    [ [0,1,1,0], [0,1,1,1], [1,1,1,0], [1,1,1,1], [2,1,1,0], [2,1,1,1], [3,1,1,0], [3,1,1,1] ]
]

let pattern4 = [
    [ [0,2,0,0], [0,2,0,1], [0,2,0,2], [0,2,0,3], [1,1,0,0], [1,1,0,1], [2,1,0,0], [2,1,0,1] ],
    [ [0,2,1,0], [0,2,1,1], [0,2,1,2], [0,2,1,3], [1,1,1,0], [1,1,1,1], [2,1,1,0], [2,1,1,1] ],
    [ [0,2,2,0], [0,2,2,1], [0,2,2,2], [0,2,2,3], [3,1,0,0], [3,1,0,1], [4,1,0,0], [4,1,0,1] ],
    [ [0,2,3,0], [0,2,3,1], [0,2,3,2], [0,2,3,3], [3,1,1,0], [3,1,1,1], [4,1,1,0], [4,1,1,1] ]
]

let pattern5 = [
    [ [0,1,0,0], [0,1,0,1], [1,1,0,0], [1,1,0,1], [4,2,0,0], [4,2,0,1], [4,2,0,2], [4,2,0,3] ],
    [ [0,1,1,0], [0,1,1,1], [1,1,1,0], [1,1,1,1], [4,2,1,0], [4,2,1,1], [4,2,1,2], [4,2,1,3] ],
    [ [2,1,0,0], [2,1,0,1], [3,1,0,0], [3,1,0,1], [4,2,2,0], [4,2,2,1], [4,2,2,2], [4,2,2,3] ],
    [ [2,1,1,0], [2,1,1,1], [3,1,1,0], [3,1,1,1], [4,2,3,0], [4,2,3,1], [4,2,3,2], [4,2,3,3] ]
]

let patterns = [
    pattern1,
    pattern2,
    pattern3,
    pattern4,
    pattern5
]

Instance.prototype.make = function (designs) {
    var random = makeRandom()
    var offset = 0
    if (designs.length == 0) {
        return []
    }
    let rows = [0, 1, 2, 3].map((idx) => {
        let patternIdx = Math.round(random() * (patterns.length - 1))
        let pattern = patterns[patternIdx]
        let advance = pattern.reduce((previousValue, row) => {
            return row.reduce((previousValue, col) => {
                return Math.max(previousValue, col[0])
            }, previousValue)
        }, 0)
        let cells = pattern.map((row) => {
            return row.map((col) => {
                let design = designs[(offset + col[0]) % designs.length]
                let url = design.baseUrl + "/" + design.uuid + "/" + col[1] + "/" + col[3] + "/" + col[2] + "/256.png?t=" + design.checksum
                let cell = {
                    design: design,
                    imageUrl: url
                }
                return cell
            })
        })
        offset += advance
        return cells
    })
    return rows
}

module.exports = new Instance()
module.exports.create = function() {
  return new Instance()
}
