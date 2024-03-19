module.exports = {
    context: __dirname + "/src/",

    entry: {
        designs: "./DesignsApp",
        design: "./DesignApp"
    },

    output: {
        path: __dirname + "/dist",
        filename: "[name].js"
    },

    resolve: {
        extensions: [
            '.js',
            '.jsx',
            '.json',
            '.html'
        ],
        modules: [
            __dirname + '/node_modules'
        ]
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
    },

    devServer: {
        static: [
            {
                directory: __dirname + '/test'
            },
            {
                directory: __dirname + '/dist'
            }
        ],
        compress: true,
        port: 8888
    }
};

