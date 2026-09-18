using System;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Text;

namespace AIDictation.Helpers;

/// <summary>
/// Resolves the foreground window's process name and title so context rules
/// can target specific applications, mirroring the macOS AppContextHelper.
/// </summary>
public static class ForegroundWindowHelper
{
    // MARK: - Types

    public readonly record struct ForegroundApp(string ProcessName, string WindowTitle);

    // MARK: - Public API

    /// <summary>
    /// Returns the foreground window handle, or IntPtr.Zero when none.
    /// </summary>
    public static IntPtr GetForegroundWindowHandle()
    {
        try
        {
            return GetForegroundWindow();
        }
        catch
        {
            return IntPtr.Zero;
        }
    }

    /// <summary>
    /// Gets the editable child control that currently owns keyboard focus in
    /// the supplied top-level window. Unlike GetFocus, this works across input
    /// threads and lets a delayed dictation result return to the original field.
    /// </summary>
    public static IntPtr GetFocusedControlHandle(IntPtr topLevelWindow)
    {
        try
        {
            if (topLevelWindow == IntPtr.Zero) return IntPtr.Zero;
            var threadId = GetWindowThreadProcessId(topLevelWindow, out _);
            if (threadId == 0) return IntPtr.Zero;

            var info = new GuiThreadInfo { cbSize = Marshal.SizeOf<GuiThreadInfo>() };
            return GetGUIThreadInfo(threadId, ref info) ? info.hwndFocus : IntPtr.Zero;
        }
        catch
        {
            return IntPtr.Zero;
        }
    }

    /// <summary>
    /// Returns the current foreground application, or null when it cannot be resolved
    /// (e.g. the desktop or a protected system window is focused).
    /// </summary>
    public static ForegroundApp? GetForegroundApp()
    {
        try
        {
            var hwnd = GetForegroundWindow();
            if (hwnd == IntPtr.Zero) return null;

            GetWindowThreadProcessId(hwnd, out var processId);
            if (processId == 0) return null;

            string processName;
            using (var process = Process.GetProcessById((int)processId))
            {
                processName = process.ProcessName;
            }

            var titleBuilder = new StringBuilder(512);
            GetWindowText(hwnd, titleBuilder, titleBuilder.Capacity);

            return new ForegroundApp(processName, titleBuilder.ToString());
        }
        catch
        {
            return null;
        }
    }

    // MARK: - Native Methods

    [DllImport("user32.dll")]
    private static extern IntPtr GetForegroundWindow();

    [DllImport("user32.dll")]
    private static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);

    [DllImport("user32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool GetGUIThreadInfo(uint idThread, ref GuiThreadInfo lpgui);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    private static extern int GetWindowText(IntPtr hWnd, StringBuilder lpString, int nMaxCount);

    [StructLayout(LayoutKind.Sequential)]
    private struct GuiThreadInfo
    {
        public int cbSize;
        public int flags;
        public IntPtr hwndActive;
        public IntPtr hwndFocus;
        public IntPtr hwndCapture;
        public IntPtr hwndMenuOwner;
        public IntPtr hwndMoveSize;
        public IntPtr hwndCaret;
        public Rect rcCaret;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct Rect
    {
        public int left;
        public int top;
        public int right;
        public int bottom;
    }
}
