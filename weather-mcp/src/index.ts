import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequest,
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { z } from "zod";

import dotenv from "dotenv";
dotenv.config({
  // IMPORTANT: Enter the full absolute path to your .env file here.
  // Claude Desktop may not find the .env file if you use a relative path.
  // Example: path: "C:/your/path/.env"
  path: "C:/your/path/.env",
});

const getWeatherSchema = z.object({
  city: z.string(),
});

//console.error("ENV URL:", process.env.WEATHER_API_URL);
//console.error("ENV KEY:", process.env.WEATHER_API_KEY);

const server = new Server(
  {
    name: "semdin-weather-mcp",
    version: "1.0.0",
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

// all avaliable tools
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      {
        name: "get-weather",
        description: "get weather info from openweather",
        inputSchema: {
          type: "object",
          properties: {
            city: {
              type: "string",
              description: "name of the city (e.g. mardin)",
            },
          },
          required: ["city"],
        },
      },
    ],
  };
});

// tool body
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  try {
    if (name === "get-weather") {
      const { city } = getWeatherSchema.parse(args);
      const reqUrl = `${process.env.WEATHER_API_URL}?key=${process.env.WEATHER_API_KEY}&q=${city}&aqi=no`;

      const data = await fetch(reqUrl);

      if (!data.ok) {
        return {
          content: [
            {
              type: "text",
              text: "Some error occured.",
            },
          ],
        };
      }

      const jsonData = (await data.json()) as any;

      return {
        content: [
          {
            type: "text",
            text: `the weather in ${city} is currently: ${jsonData.current.temp_c}`,
          },
        ],
      };
    } else {
      return {
        content: [
          {
            type: "text",
            text: "unknown tool",
          },
        ],
      };
    }
  } catch (error) {
    const err = error as any;

    throw new Error(err.message);
  }
});

const transport = new StdioServerTransport();
await server.connect(transport);
