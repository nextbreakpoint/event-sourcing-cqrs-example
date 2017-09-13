module.exports = {
    context: __dirname + "/",

    entry: {
        designs: "./DesignsApp",
        preview: "./PreviewApp"
    },

    output: {
        path: __dirname + "/../resources/js",
        filename: "[name].js"
    },

    resolve: {
        extensions: ['.js', '.jsx', '.json']
    },

    node: {
        fs: "empty"
    },

    module: {
        rules: [
            {
                test: /\.jsx?$/, exclude: /node_modules/, loaders: ["babel-loader"]
            }
        ]
    }
};
