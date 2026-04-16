import { BrowserRouter } from "react-router-dom";
import FlowCopilotLayout from "./components/FlowCopilotLayout.tsx";
import { ChatSessionsProvider } from "./contexts/ChatSessionsContext.tsx";

function App() {
  return (
    <BrowserRouter>
      <ChatSessionsProvider>
        <FlowCopilotLayout />
      </ChatSessionsProvider>
    </BrowserRouter>
  );
}

export default App;
