# Project Description
The proxy enables integration between OpenAI chat models and local MCP servers via [Function Calling](https://platform.openai.com/docs/guides/function-calling), allowing 
custom PII checks for sensitive data verification

# Local Run
_the Default example will work with the [filesystem](https://github.com/modelcontextprotocol/servers/tree/main/src/filesystem) mcp server and requires it to be pre-installed as docker image. 
However, one can configure any count of other mcp servers by changing `data/McpConfig.conf` file_
- Set your openAI api key to the docker compose file, `OPEN_AI_API_KEY` variable
- Set an absolute path to the folder shared with the model to the docker compose file, `SHARED_DIRECTORY_ABSOLUTE_PATH` variable
- run docker compose up
- open http://localhost:4173/ and start chat with the model

# MCP Proxy MVP Specification

- Provide the ability to make MCP calls for OpenAI models
- Run PII checks for the data provided to LLM
- Ask the user for access to share sensitive information with LLM
- No authentication mechanism
- No metrics
- No strict SLA's

**Prerequisites**
* OpenAI chat models with tool‑calling support.


**Tradeoffs**
- The app is stateful, with an in-memory persistence layer: 
  - In the case of a crash or a restart, users' workflow will be lost
  - The app is not horizontally scalable
- Supported MCP servers are specified via configuration and stored in-memory. 
  - Adding a new MCP server requires a restart.
- PII detection supports EN language only
- If sensitive data is found, it will not be provided to users for direct verification. \
  Users will only know that the data is sensitive, and they can either grant LLM access or deny it
- If the data retrieved from the MCP servers is out of the context window, the app will return an error to the user and stop the dialog

**Full Sequence Diagram for success path**
```mermaid
sequenceDiagram
    autonumber
    participant User
    participant Proxy
    participant LLM
    participant MCP
    participant PII

    User ->> Proxy : Send Message to LLM
    Proxy ->> LLM : Send user message + tool list

    LLM ->> Proxy : Send request to use tools
    Proxy ->> MCP  : Call needed tools
    MCP ->> Proxy : Return results

    Proxy ->> PII: Test data
    PII ->> Proxy: Send test results
    Proxy ->> User: Ask to share data
    User ->> Proxy: Give access 


    Proxy ->> LLM : send message with tool results
    LLM ->> Proxy : send final response
    Proxy ->> User: send final response
```

In the case if the user doesn't want to share sensitive information, the entire dialog will be stopped:

```mermaid
sequenceDiagram
    autonumber
    participant User
    participant Proxy
    participant LLM
    participant MCP
    participant PII
    Proxy ->> PII: Test data
    PII ->> Proxy: Send test results
    Proxy ->> User: Ask to share data
    User ->> Proxy: Do not give access
    Proxy ->> User: Send acknowledge 
```

### MCP injection
* Data about available MCP servers will be provided to model using [Function Calling](https://platform.openai.com/docs/guides/function-calling)
* _Tools_ feature is supported in the [official Java SDK](https://github.com/openai/openai-java) which is going to be used for interactions with the model
* [Official MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) will be used for communications with MCP server

### PII module
* PII check will be based on regex and encapsulated for future extensions
* PII module just verify if data is sensitive, it will not mask or remove data from the request
