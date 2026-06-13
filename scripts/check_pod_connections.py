import subprocess
import json
import re

def run_cmd(cmd):
    try:
        res = subprocess.run(cmd, shell=True, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        return res.stdout.strip()
    except subprocess.CalledProcessError as e:
        print(f"Error running cmd: {cmd}\nstderr: {e.stderr}")
        return ""

def get_pods():
    output = run_cmd("oc get pods -n payu-dev -o json")
    if not output:
        return []
    try:
        data = json.loads(output)
        return [item['metadata']['name'] for item in data['items'] if 'app' in item['metadata'].get('labels', {}) or 'app.kubernetes.io/name' in item['metadata'].get('labels', {})]
    except Exception as e:
        print(f"Failed to parse pods JSON: {e}")
        return []

def analyze_logs(pod_name):
    print(f"Analyzing logs for pod: {pod_name}...")
    logs = run_cmd(f"oc logs {pod_name} -n payu-dev --tail=300")
    if not logs:
        # Try with container name 'app'
        logs = run_cmd(f"oc logs {pod_name} -n payu-dev -c app --tail=300")
    
    status = {
        "sso": "N/A",
        "kafka": "N/A",
        "broker": "N/A",
        "cache": "N/A",
        "status": "Running"
    }

    # If log is empty, pod might be starting or crashlooping
    if not logs:
        status["status"] = "No logs / Starting"
        return status

    # Check for startup errors/crashes
    if "Exception" in logs or "Error" in logs or "FATAL" in logs or "PatternParseException" in logs:
        status["status"] = "Error / Exceptions found"

    # Analyze SSO / OIDC / Keycloak
    if any(k in logs.lower() for k in ["oidc", "keycloak", "jwk", "auth-server"]):
        if any(k in logs.lower() for k in ["oidc provider initialized", "jwk set loaded", "initialized successfully", "connected to keycloak"]):
            status["sso"] = "🟢 Connected"
        elif any(k in logs.lower() for k in ["failed to initialize jwt processor", "connection refused", "jwks uri may be unavailable"]):
            status["sso"] = "🔴 Failed / Unreachable"
        else:
            status["sso"] = "🟡 Configured"

    # Analyze Kafka
    if any(k in logs.lower() for k in ["kafka", "bootstrap-server"]):
        if any(k in logs.lower() for k in ["metadata update", "producer client", "consumer client", "subscribed to topic", "connected"]):
            status["kafka"] = "🟢 Connected"
        elif any(k in logs.lower() for k in ["disconnect", "connection refused", "timed out", "unreachable", "error"]):
            status["kafka"] = "🔴 Failed / Disconnected"
        else:
            status["kafka"] = "🟡 Configured"

    # Analyze Broker (Artemis / JMS)
    if any(k in logs.lower() for k in ["artemis", "activemq", "jms", "stomp"]):
        if any(k in logs.lower() for k in ["established connection", "connected", "stomp connection", "jms connection"]):
            status["broker"] = "🟢 Connected"
        elif any(k in logs.lower() for k in ["connection refused", "could not connect", "failed", "error"]):
            status["broker"] = "🔴 Failed / Unreachable"
        else:
            status["broker"] = "🟡 Configured"

    # Analyze Datagrid / Redis / Cache
    if any(k in logs.lower() for k in ["redis", "lettuce", "jedis", "datagrid", "cache"]):
        if any(k in logs.lower() for k in ["connected to redis", "redis connection established", "lettuce connection", "cache initialized"]):
            status["cache"] = "🟢 Connected"
        elif any(k in logs.lower() for k in ["connection refused", "cannot connect", "redisconnectionexception", "error", "unreachable"]):
            status["cache"] = "🔴 Failed / Unreachable"
        else:
            status["cache"] = "🟡 Configured"

    return status

def main():
    pods = get_pods()
    if not pods:
        print("No app pods found.")
        return

    results = {}
    for pod in pods:
        results[pod] = analyze_logs(pod)

    # Print markdown table
    print("\n\n### Pod Connection Status Report\n")
    print("| Pod Name | Pod Status | SSO / Keycloak | Kafka | AMQ Broker | Redis / Cache |")
    print("|---|---|---|---|---|---|")
    for pod, stat in sorted(results.items()):
        print(f"| `{pod}` | {stat['status']} | {stat['sso']} | {stat['kafka']} | {stat['broker']} | {stat['cache']} |")

if __name__ == "__main__":
    main()
