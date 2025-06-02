/**
 * NutriGraph AI Chatbot
 * A floating chatbot that uses the Gemini API to provide food-related assistance
 */

class NutriGraphChatbot {
    constructor(apiKey) {
        this.apiKey = apiKey;
        this.messages = [];
        this.isOpen = false;
        this.init();
    }

    init() {
        // Create chatbot elements
        this.createChatbotElements();
        
        // Add event listeners
        this.addEventListeners();
        
        // Add initial bot message
        this.addBotMessage("Hello! I'm NutriBot, your food intelligence assistant. Ask me anything about nutrition, ingredients, cooking methods, or cultural food contexts!");
    }

    createChatbotElements() {
        // Create styles
        const style = document.createElement('style');
        style.textContent = `
            .nutrigraph-chatbot-button {
                position: fixed;
                bottom: 20px;
                right: 20px;
                width: 60px;
                height: 60px;
                border-radius: 50%;
                background: var(--primary-gradient, linear-gradient(135deg, #6b46c1 0%, #9f7aea 100%));
                box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
                display: flex;
                align-items: center;
                justify-content: center;
                cursor: pointer;
                z-index: 9998;
                transition: all 0.3s ease;
            }
            
            .nutrigraph-chatbot-button:hover {
                transform: scale(1.05);
            }
            
            .nutrigraph-chatbot-button i {
                color: white;
                font-size: 24px;
            }
            
            .nutrigraph-chatbot-widget {
                position: fixed;
                bottom: 90px;
                right: 20px;
                width: 350px;
                height: 500px;
                background: rgba(255, 255, 255, 0.9);
                backdrop-filter: blur(10px);
                border-radius: 15px;
                box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
                display: flex;
                flex-direction: column;
                overflow: hidden;
                z-index: 9999;
                transition: all 0.3s ease;
                opacity: 0;
                pointer-events: none;
                transform: translateY(20px);
            }
            
            .nutrigraph-chatbot-widget.open {
                opacity: 1;
                pointer-events: all;
                transform: translateY(0);
            }
            
            .nutrigraph-chatbot-header {
                background: var(--primary-gradient, linear-gradient(135deg, #6b46c1 0%, #9f7aea 100%));
                padding: 15px 20px;
                color: white;
                display: flex;
                justify-content: space-between;
                align-items: center;
            }
            
            .nutrigraph-chatbot-title {
                display: flex;
                align-items: center;
                gap: 10px;
            }
            
            .nutrigraph-chatbot-title i {
                font-size: 20px;
            }
            
            .nutrigraph-chatbot-title h3 {
                margin: 0;
                font-size: 16px;
                font-weight: 600;
            }
            
            .nutrigraph-chatbot-close {
                cursor: pointer;
                font-size: 18px;
            }
            
            .nutrigraph-chatbot-messages {
                flex: 1;
                padding: 20px;
                overflow-y: auto;
                display: flex;
                flex-direction: column;
                gap: 15px;
            }
            
            .nutrigraph-chatbot-message {
                max-width: 80%;
                padding: 12px 15px;
                border-radius: 15px;
                font-size: 14px;
                line-height: 1.4;
            }
            
            .nutrigraph-chatbot-message.bot {
                align-self: flex-start;
                background: #f0f0f0;
                border-bottom-left-radius: 5px;
            }
            
            .nutrigraph-chatbot-message.user {
                align-self: flex-end;
                background: var(--primary-gradient, linear-gradient(135deg, #6b46c1 0%, #9f7aea 100%));
                color: white;
                border-bottom-right-radius: 5px;
            }
            
            .nutrigraph-chatbot-input {
                padding: 15px;
                border-top: 1px solid rgba(0, 0, 0, 0.1);
                display: flex;
                gap: 10px;
            }
            
            .nutrigraph-chatbot-input input {
                flex: 1;
                padding: 10px 15px;
                border: 1px solid rgba(0, 0, 0, 0.1);
                border-radius: 20px;
                outline: none;
                font-size: 14px;
            }
            
            .nutrigraph-chatbot-send {
                width: 40px;
                height: 40px;
                border-radius: 50%;
                background: var(--primary-gradient, linear-gradient(135deg, #6b46c1 0%, #9f7aea 100%));
                color: white;
                display: flex;
                align-items: center;
                justify-content: center;
                cursor: pointer;
                transition: all 0.2s ease;
            }
            
            .nutrigraph-chatbot-send:hover {
                transform: scale(1.05);
            }
            
            .nutrigraph-chatbot-loading {
                display: flex;
                gap: 5px;
                align-items: center;
                padding: 10px 15px;
                background: #f0f0f0;
                border-radius: 15px;
                border-bottom-left-radius: 5px;
                align-self: flex-start;
                font-size: 14px;
                max-width: 80%;
            }
            
            .nutrigraph-chatbot-dot {
                width: 8px;
                height: 8px;
                background: #888;
                border-radius: 50%;
                animation: nutrigraph-chatbot-bounce 1.4s infinite ease-in-out both;
            }
            
            .nutrigraph-chatbot-dot:nth-child(1) {
                animation-delay: -0.32s;
            }
            
            .nutrigraph-chatbot-dot:nth-child(2) {
                animation-delay: -0.16s;
            }
            
            @keyframes nutrigraph-chatbot-bounce {
                0%, 80%, 100% {
                    transform: scale(0);
                } 40% {
                    transform: scale(1);
                }
            }
        `;
        document.head.appendChild(style);

        // Create button
        this.button = document.createElement('div');
        this.button.className = 'nutrigraph-chatbot-button';
        this.button.innerHTML = '<i class="fas fa-robot"></i>';
        document.body.appendChild(this.button);

        // Create widget
        this.widget = document.createElement('div');
        this.widget.className = 'nutrigraph-chatbot-widget';
        this.widget.innerHTML = `
            <div class="nutrigraph-chatbot-header">
                <div class="nutrigraph-chatbot-title">
                    <i class="fas fa-brain"></i>
                    <h3>NutriBot Assistant</h3>
                </div>
                <div class="nutrigraph-chatbot-close">
                    <i class="fas fa-times"></i>
                </div>
            </div>
            <div class="nutrigraph-chatbot-messages"></div>
            <div class="nutrigraph-chatbot-input">
                <input type="text" placeholder="Ask about food, nutrition, or recipes...">
                <div class="nutrigraph-chatbot-send">
                    <i class="fas fa-paper-plane"></i>
                </div>
            </div>
        `;
        document.body.appendChild(this.widget);

        // Get elements
        this.messagesContainer = this.widget.querySelector('.nutrigraph-chatbot-messages');
        this.inputField = this.widget.querySelector('.nutrigraph-chatbot-input input');
        this.sendButton = this.widget.querySelector('.nutrigraph-chatbot-send');
        this.closeButton = this.widget.querySelector('.nutrigraph-chatbot-close');
    }

    addEventListeners() {
        // Toggle chatbot
        this.button.addEventListener('click', () => this.toggleChatbot());
        
        // Close chatbot
        this.closeButton.addEventListener('click', () => this.toggleChatbot(false));
        
        // Send message on button click
        this.sendButton.addEventListener('click', () => this.sendMessage());
        
        // Send message on Enter key
        this.inputField.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.sendMessage();
            }
        });
    }

    toggleChatbot(forceState = null) {
        this.isOpen = forceState !== null ? forceState : !this.isOpen;
        
        if (this.isOpen) {
            this.widget.classList.add('open');
            setTimeout(() => this.inputField.focus(), 300);
        } else {
            this.widget.classList.remove('open');
        }
    }

    sendMessage() {
        const message = this.inputField.value.trim();
        
        if (!message) return;
        
        // Add user message to chat
        this.addUserMessage(message);
        
        // Clear input field
        this.inputField.value = '';
        
        // Show loading indicator
        this.showLoadingIndicator();
        
        // Send to Gemini API
        this.sendToGemini(message);
    }

    addUserMessage(text) {
        const messageElement = document.createElement('div');
        messageElement.className = 'nutrigraph-chatbot-message user';
        messageElement.textContent = text;
        this.messagesContainer.appendChild(messageElement);
        this.scrollToBottom();
        
        // Add to messages array
        this.messages.push({
            role: 'user',
            content: text
        });
    }

    addBotMessage(text) {
        const messageElement = document.createElement('div');
        messageElement.className = 'nutrigraph-chatbot-message bot';
        messageElement.textContent = text;
        this.messagesContainer.appendChild(messageElement);
        this.scrollToBottom();
        
        // Add to messages array
        this.messages.push({
            role: 'bot',
            content: text
        });
    }

    showLoadingIndicator() {
        const loadingElement = document.createElement('div');
        loadingElement.className = 'nutrigraph-chatbot-loading';
        loadingElement.innerHTML = `
            <div class="nutrigraph-chatbot-dot"></div>
            <div class="nutrigraph-chatbot-dot"></div>
            <div class="nutrigraph-chatbot-dot"></div>
        `;
        this.messagesContainer.appendChild(loadingElement);
        this.scrollToBottom();
        
        // Store reference to remove later
        this.loadingIndicator = loadingElement;
    }

    removeLoadingIndicator() {
        if (this.loadingIndicator && this.loadingIndicator.parentNode) {
            this.loadingIndicator.parentNode.removeChild(this.loadingIndicator);
            this.loadingIndicator = null;
        }
    }

    scrollToBottom() {
        this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
    }

    async sendToGemini(message) {
        try {
            // Prepare the context and prompt with fallback mechanism
            const prompt = `You are NutriBot, an AI assistant for the NutriGraph food intelligence platform. 
            Your purpose is to help users with questions about food, nutrition, ingredients, cooking methods, 
            and cultural food contexts. Be friendly, informative, and concise.
            
            User query: ${message}`;
            
            // Set a timeout to ensure we don't wait too long for the API
            const timeoutPromise = new Promise((_, reject) => {
                setTimeout(() => reject(new Error('Request timeout')), 10000);
            });
            
            // Create the fetch promise
            const fetchPromise = fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${this.apiKey}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    contents: [
                        {
                            parts: [
                                {
                                    text: prompt
                                }
                            ]
                        }
                    ]
                })
            });
            
            // Race the fetch against the timeout
            const response = await Promise.race([fetchPromise, timeoutPromise]);
            const data = await response.json();
            
            // Remove loading indicator
            this.removeLoadingIndicator();
            
            // Check if we have a valid response
            if (data.candidates && data.candidates[0] && data.candidates[0].content) {
                const botResponse = data.candidates[0].content.parts[0].text;
                this.addBotMessage(botResponse);
            } else {
                // Handle API error with helpful fallback
                this.handleApiError(message);
            }
        } catch (error) {
            // Remove loading indicator
            this.removeLoadingIndicator();
            
            // Handle different error types
            this.handleApiError(message, error);
        }
    }
    
    handleApiError(userMessage, error = null) {
        console.error("Chatbot Error:", error);
        
        // Try to provide a helpful response based on the user's query
        // even when the API is unavailable
        const lowerMessage = userMessage.toLowerCase();
        
        // Check for common food-related queries
        if (lowerMessage.includes('recipe') || lowerMessage.includes('how to cook') || lowerMessage.includes('how to make')) {
            this.addBotMessage(
                "I'm currently having trouble connecting to my knowledge base. " +
                "For recipes and cooking instructions, you can try using the search feature above to find specific foods, " +
                "or check our nutrition section for healthy meal ideas."
            );
        } else if (lowerMessage.includes('nutrition') || lowerMessage.includes('calories') || lowerMessage.includes('protein')) {
            this.addBotMessage(
                "I'm currently having trouble connecting to my knowledge base. " +
                "For nutrition information, try searching for a specific food in our search bar above, " +
                "or explore the nutrition tab for detailed nutritional data."
            );
        } else if (lowerMessage.includes('culture') || lowerMessage.includes('tradition') || lowerMessage.includes('origin')) {
            this.addBotMessage(
                "I'm currently having trouble connecting to my knowledge base. " +
                "To learn about food cultures and traditions, try our Cultural Explorer tab " +
                "where you can discover foods from different regions and their cultural significance."
            );
        } else {
            this.addBotMessage(
                "I'm sorry, I'm having trouble connecting to my knowledge base right now. " +
                "While I'm getting back online, feel free to use the search features above to explore foods, " +
                "or try again in a few moments."
            );
        }
    }
}

// Initialize the chatbot when the page is fully loaded
document.addEventListener('DOMContentLoaded', () => {
    // Check if we already have a chatbot instance
    if (window.nutriGraphChatbot) return;
    
    // Create a new chatbot instance
    // Note: Replace 'YOUR_GEMINI_API_KEY' with your actual API key
    window.nutriGraphChatbot = new NutriGraphChatbot('YOUR_GEMINI_API_KEY');
});
