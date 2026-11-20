package proxyflowwrapper

import (
	"net"
	"sync"

	"github.com/etidart/proxyflow_android/proxyflow_wrapper/internal/checker"
	"github.com/etidart/proxyflow_android/proxyflow_wrapper/internal/logging"
	"github.com/etidart/proxyflow_android/proxyflow_wrapper/internal/proxy"
	"github.com/etidart/proxyflow_android/proxyflow_wrapper/internal/server"
)

var (
	listenerMu sync.Mutex
	listener   net.Listener
)

func StartSocks5(listenon string, pfile string, checkingn int) string {
	listenerMu.Lock()
	defer listenerMu.Unlock()

	if listener != nil {
		_ = listener.Close()
		listener = nil
	}

	logging.Init()

	if pfile == "" {
		return "crit: no proxy file specified"
	}

	pm := proxy.NewProxyManager()
	err := pm.ParseFile(pfile)
	if err != nil {
		return "crit: failed to parse proxy file: " + err.Error()
	}
	checker.StartChecking(checkingn, pm)

	proxiesChannel := make(chan chan proxy.Message)
	go pm.ServeProxies(proxiesChannel)

	listener, err := net.Listen("tcp", listenon)
	if err != nil {
		return "crit: failed to start listener: " + err.Error()
	}

	activeListener := listener
	go func() {
		server.ListenAndServe(activeListener, proxiesChannel)
	}()
	return ""
}

func StopSocks5() string {
	listenerMu.Lock()
	defer listenerMu.Unlock()

	if listener == nil {
		return "crit: listener not started"
	}
	err := listener.Close()
	if err != nil {
		return "crit: failed to stop listener: " + err.Error()
	}
	listener = nil
	return ""
}
