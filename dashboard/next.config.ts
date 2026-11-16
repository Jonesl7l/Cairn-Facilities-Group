import type { NextConfig } from 'next'

// The dashboard is a single client page talking to the mock server.
// agentRules: false stops next dev writing an AGENTS.md for AI coding agents into the project
const nextConfig: NextConfig = {
  agentRules: false,
}

export default nextConfig
