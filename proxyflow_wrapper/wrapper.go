package proxyflowwrapper

import (
	"net"

	"github.com/etidart/proxyflow_android/proxyflow_wrapper/internal/checker"
	"github.com/etidart/proxyflow_android/proxyflow_wrapper/internal/logging"
	"github.com/etidart/proxyflow_android/proxyflow_wrapper/internal/proxy"
	"github.com/etidart/proxyflow_android/proxyflow_wrapper/internal/server"
)

var listener net.Listener

func StartSocks5(listenon string, pfile string, checkingn int) string {
	if listener != nil {
		return "crit: listener already started"
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

	go func() {
		server.ListenAndServe(listener, proxiesChannel)
	}()
	return ""
}

func StopSocks5() string {
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
