import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        InitKoinKt.doInitKoin()
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)
                .ignoresSafeArea(.keyboard)
        }
    }
}
