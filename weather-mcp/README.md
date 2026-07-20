# MCP Weather - Claude Desktop Integration

This project is an example MCP (Model Context Protocol) server designed for use with the Claude Desktop application. It is built to work seamlessly with Claude Desktop.

## Features

- TypeScript-based MCP server
- Weather query via OpenWeather API
- Easy integration with Claude Desktop

## Project Structure

```
.
├── package.json
├── tsconfig.json
├── src/
│   └── index.ts
├── claude_desktop_config.json
```

## Installation

### Requirements

- Node.js (v16 or higher)
- npm
- Claude Desktop (https://desktop.anthropic.com/)

### Setup

1. Clone the repository:
   ```sh
   git clone <repository-url>
   cd mcp-weather
   ```
2. Install dependencies:
   ```sh
   npm install
   ```
3. Create a `.env` file and fill it as follows:
   ```env
   WEATHER_API_URL=https://api.weatherapi.com/v1/current.json
   WEATHER_API_KEY=<your_API_KEY>
   ```

## Claude Desktop Integration

1. In Claude Desktop, go to `File -> Settings -> Developer -> Edit Config`.
2. Add a block similar to the example below to the config file (edit the file paths to match your own system):

   ```json
   {
     "mcpServers": {
       "weather": {
         "command": "npx",
         "args": ["tsx", "C:/your/path/mcp-weather/src/index.ts"],
         "cwd": "C:/your/path/mcp-weather"
       }
     }
   }
   ```

   > Note: Replace `C:/your/path/` with the actual path to your project on your computer.

3. Save the settings and restart Claude Desktop.

## .env File and Path Setting

In `src/index.ts`, the path to the `.env` file is set to the project root by default:

```typescript
import dotenv from "dotenv";
dotenv.config({
  // Path to your .env file. Edit as needed, e.g.: path: "C:/your/path/.env"
  path: ".env", // By default, uses the .env file in the project root
});
```

If your `.env` file is in a different directory, update the `path` parameter accordingly.

## Usage

To start the project:

```sh
npm start
```

Or to run the main file directly:

```sh
npx ts-node src/index.ts
```

## Build

To compile the TypeScript code:

```sh
npm run build
```

## Contributing

Contributions are welcome! Please open issues or submit pull requests for improvements or bug fixes.

## License

MIT License
