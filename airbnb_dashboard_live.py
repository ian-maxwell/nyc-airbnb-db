import streamlit as st
import subprocess
import time
import fcntl
import os
import signal
import html
import re

# Inject custom CSS for tighter vertical spacing
st.markdown("""
<style>
    /* Target the main app container for overall tightness */
    .stApp {
        padding-top: 10px;
        padding-bottom: 10px;
    }
    /* Target specific Streamlit elements to reduce margin/padding */
    .stMarkdown, .stSubheader, .stForm, .stButton, .stTextInput, .element-container {
        margin-top: 0px !important;
        margin-bottom: 5px !important;
        padding-top: 0px !important;
        padding-bottom: 0px !important;
    }
    /* Reduce the space between the Title and the first element */
    h1 {
        margin-bottom: 10px !important;
    }
    /* Reduce spacing for subheaders (like Quick Actions) */
    h3 {
        margin-top: 10px !important;
        margin-bottom: 5px !important;
    }
    /* Adjust specific form/button spacing */
    .stForm > div {
        margin-bottom: 0px !important;
    }
</style>
""", unsafe_allow_html=True)


st.set_page_config(page_title="NYC Airbnb Persistent Console", layout="wide")

# 🔐 SECURITY CONFIGURATION
ADMIN_PASSWORD = "YOUR_ADMIN_PASSWORD_HERE" 
GUEST_PASSWORD = "guest"

# INITIALIZE SESSION STATE
if "auth_role" not in st.session_state:
    st.session_state.auth_role = None
if "history" not in st.session_state:
    st.session_state.history = ""
if "current_page" not in st.session_state:
    st.session_state.current_page = 1
# last_query_supports_paging is REMOVED to keep buttons always visible


def check_login():
    pwd = st.session_state.password_input
    if pwd == ADMIN_PASSWORD:
        st.session_state.auth_role = "admin"
    elif pwd == GUEST_PASSWORD:
        st.session_state.auth_role = "guest"
    else:
        st.error("❌ Incorrect Password")

if not st.session_state.auth_role:
    st.markdown("## 🔒 Airbnb Console Login")
    st.markdown("Please log in to access the database terminal.")
    
    col1, col2 = st.columns([1, 2])
    with col1:
        st.text_input("Enter Password:", type="password", key="password_input", on_change=check_login)
    
    st.info(f"💡 **Tip for Recruiters:** Use password `{GUEST_PASSWORD}` for read-only access.")
    st.stop()

#  The code below only runs AFTER successful login
st.title("💻 Airbnb Persistent Terminal")
st.markdown("This terminal **remembers your session**.")

if st.session_state.auth_role == "guest":
    st.warning("👀 You are in **GUEST MODE**. Write operations (DROP, INSERT, DELETE) are disabled.")
else:
    st.success("🔓 You are in **ADMIN MODE**. Full access granted.")


# THE PERSISTENT PROCESS MANAGER
@st.cache_resource
class JavaSession:
    def __init__(self):
        self.process = subprocess.Popen(
            ["java", "-Xmx512m", "-cp", ".:mssql-jdbc-13.2.1.jre11.jar", "SQLServerMain"],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            bufsize=0 
        )
        fd = self.process.stdout.fileno()
        fl = fcntl.fcntl(fd, fcntl.F_GETFL)
        fcntl.fcntl(fd, fcntl.F_SETFL, fl | os.O_NONBLOCK)
        self.read_until_prompt()

    def send_command(self, cmd):
        if self.process.poll() is not None:
            return "Error: Java process has died. Please click 'Reset Connection'."
        self.process.stdin.write(cmd + "\n")
        self.process.stdin.flush()
        return self.read_until_prompt(cmd)

    def read_until_prompt(self, last_cmd=""):
        output = []
        time.sleep(0.1) 
        start_time = time.time()
        TIMEOUT_SECONDS = 15 
        
        while True:
            if time.time() - start_time > TIMEOUT_SECONDS:
                output.append("\n[Timeout waiting for response...]")
                break
            try:
                chunk = self.process.stdout.read()
                if chunk:
                    output.append(chunk)
                    if "db >" in chunk:
                        break
            except Exception:
                time.sleep(0.05)
                continue
                
        full_text = "".join(output)
        
        # PAGE STATE UPDATE LOGIC
        cmd_lower = last_cmd.strip().lower()
        
        if cmd_lower.startswith("page "):
            # If the last command was a page command, update the current page number
            try:
                page_num = int(cmd_lower.split(' ')[1])
                st.session_state.current_page = page_num
            except (ValueError, IndexError):
                # If command was invalid (e.g., 'page bad'), page state stays the same
                pass
        else:
            # If the command was ANY OTHER command, reset page to 1
            st.session_state.current_page = 1
            
        # RAW MODE FIX
        if full_text.strip().endswith("db >"):
            full_text = full_text.rsplit("db >", 1)[0]
            
        return full_text.rstrip()

    def kill(self):
        self.process.terminate()

# INITIALIZE SESSION
try:
    session = JavaSession()
    if not st.session_state.history:
        st.toast("Connected to Java Backend")
except Exception as e:
    st.error(f"Failed to start Java: {e}")

#SIDEBAR CONTROLS
st.sidebar.header(f"Controls ({st.session_state.auth_role.upper()})")

if st.sidebar.button("🔴 Reset Connection"):
    st.cache_resource.clear()
    session.kill()
    st.session_state.history = "" 
    st.session_state.auth_role = None 
    st.session_state.current_page = 1
    st.rerun()

if st.sidebar.button("📜 Get commands"):
    with st.spinner("Fetching help..."):
        response = session.send_command("h")
        new_entry = f"> h\n{response}\n" + "-"*40 + "\n"
        st.session_state.history = new_entry + st.session_state.history

# MAIN INTERFACE

# Helper for processing command submission
def process_command_submission(user_input):
    # 1. Check for Dangerous Keywords if Guest
    dangerous_keywords = ["drop", "delete", "insert", "update", "alter", "truncate", "create"]
    
    is_dangerous = False
    
    cmd_stripped = user_input.strip().lower()
    # Check for single 'd' or single 'r' (delete/repopulate)
    if cmd_stripped == "d" or cmd_stripped == "r":
        is_dangerous = True
    
    # Check for other dangerous keywords
    if st.session_state.auth_role == "guest" or is_dangerous:
        cmd_lower = user_input.lower()
        if any(bad in cmd_lower for bad in dangerous_keywords):
            is_dangerous = True
    
    if is_dangerous:
        error_msg = f"🚫 Permission Denied: Guest users cannot use destructive commands. (Check for 'd' or 'r' commands)."
        st.error(error_msg)
        new_entry = f"> {user_input}\n{error_msg}\n" + "-"*40 + "\n"
        st.session_state.history = new_entry + st.session_state.history
        
    else:
        # 2. Safe to Execute
        with st.spinner(f"Sending '{user_input}'..."):
            response = session.send_command(user_input)
            new_entry = f"> {user_input}\n{response}\n" + "-"*40 + "\n"
            st.session_state.history = new_entry + st.session_state.history

# 1. COMMAND INPUT FORM
with st.form(key='console_form', clear_on_submit=True):
    col1, col2 = st.columns([4, 1])
    with col1:
        user_input = st.text_input("Enter Command (or use buttons below):", key="cmd_input")
    with col2:
        st.write("") 
        st.write("") 
        submit_button = st.form_submit_button(label='Send')

# PAGING BUTTONS (ALWAYS VISIBLE)
st.markdown(f"### 📄 Page Controls (Current Page: {st.session_state.current_page})")
p_col1, p_col2, p_col3 = st.columns([1, 1, 4])

next_page = st.session_state.current_page + 1
prev_page = st.session_state.current_page - 1

with p_col1:
    # Previous Page Button (Always sends command, even if page 1. Java handles bad requests.)
    if p_col1.button("⬅️ Previous Page"):
        page_to_send = max(1, prev_page) # Do not send page 0 or negative
        process_command_submission(f"page {page_to_send}")

with p_col2:
    # Next Page Button (Always sends command)
    if p_col2.button("Next Page ➡️"):
        if next_page > 1000:
             st.warning("⚠️ Warning: Page number is very high. Proceeding anyway.")
        process_command_submission(f"page {next_page}")

st.markdown("---") 

# 2. QUICK ACTION BUTTONS (5 Columns)
st.markdown("### ⚡ Quick Actions")
q_col1, q_col2, q_col3, q_col4, q_col5 = st.columns(5)

def run_quick_command(cmd_text, display_name):
    process_command_submission(cmd_text)

# Assign one button per column
with q_col1:
    if st.button("👥 Query All Hosts"):
        run_quick_command("allhosts", "All Hosts")

with q_col2:
    if st.button("💰 Listing Finances"):
        run_quick_command("listingfinances", "Finances")

with q_col3:
    if st.button("🚗 Listings & Collisions"):
        run_quick_command("listingsandcollisions", "Listings & Collisions")

with q_col4:
    if st.button("📍 Neighbourhoods per Borough"):
        run_quick_command("neighbourhoodperborough", "Neighbourhoods")

with q_col5:
    if st.button("🏊 List Amenities"):
        run_quick_command("amenities", "Amenities")


# EXECUTION LOGIC FOR MANUAL INPUT
if submit_button and user_input:
    process_command_submission(user_input)

# DISPLAY OUTPUT
st.divider()
st.subheader("Session Log (Newest first):")

st.markdown(
    f"""
    <div style="
        height: 600px;
        overflow-y: scroll;
        background-color: #0e1117;
        border: 1px solid #4a4a4a;
        border-radius: 5px;
        padding: 10px;
        color: #fafafa;
        font-family: 'Courier New', monospace;
        white-space: pre; 
    ">
    {html.escape(st.session_state.history)}
    </div>
    """,
    unsafe_allow_html=True
)
