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
        extensions: ['.js', '.jsx', '.json', '.html']
    },

    module: {
        rules: [
          {
            test: /\.jsx?$/,
            exclude: /node_modules/,
            use: {
              loader: "babel-loader"
            }
          },
          {
            test: /\.html$/,
            use: [
              {
                loader: "html-loader"
              }
            ]
          }
        ]
    }
};

