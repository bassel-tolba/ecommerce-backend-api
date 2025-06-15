import sys
import os
import threading
from qtpy import QtWidgets, QtCore, QtGui

# --- Style Sheets (QSS) for a modern look ---

DARK_STYLE = """
    QWidget {
        background-color: #2b2b2b;
        color: #f0f0f0;
        font-family: Arial, sans-serif;
    }
    QMainWindow {
        background-color: #2b2b2b;
    }
    QTextEdit {
        background-color: #3c3c3c;
        border: 1px solid #4d4d4d;
        border-radius: 5px;
        padding: 5px;
        font-size: 14px;
    }
    QPushButton {
        background-color: #555555;
        border: 1px solid #666666;
        padding: 8px;
        border-radius: 5px;
        font-size: 14px;
    }
    QPushButton:hover {
        background-color: #6a6a6a;
        border: 1px solid #777777;
    }
    QPushButton:pressed {
        background-color: #4a4a4a;
    }
    QPushButton:disabled {
        background-color: #404040;
        color: #888888;
    }
    QStatusBar {
        font-size: 12px;
    }
    QProgressBar {
        border: 1px solid #666666;
        border-radius: 5px;
        text-align: center;
        background-color: #3c3c3c;
    }
    QProgressBar::chunk {
        background-color: #007acc;
        border-radius: 4px;
    }
    QCheckBox::indicator {
        width: 18px;
        height: 18px;
    }
    QCheckBox::indicator:unchecked {
        background-color: #3c3c3c;
        border: 1px solid #666;
        border-radius: 4px;
    }
    QCheckBox::indicator:checked {
        background-color: #007acc;
        border: 1px solid #007acc;
        border-radius: 4px;
    }
"""

LIGHT_STYLE = """
    QWidget {
        background-color: #f0f0f0;
        color: #000000;
        font-family: Arial, sans-serif;
    }
    QMainWindow {
        background-color: #f0f0f0;
    }
    QTextEdit {
        background-color: #ffffff;
        border: 1px solid #cccccc;
        border-radius: 5px;
        padding: 5px;
        font-size: 14px;
    }
    QPushButton {
        background-color: #e1e1e1;
        border: 1px solid #adadad;
        padding: 8px;
        border-radius: 5px;
        font-size: 14px;
    }
    QPushButton:hover {
        background-color: #e9e9e9;
        border: 1px solid #b7b7b7;
    }
    QPushButton:pressed {
        background-color: #d1d1d1;
    }
    QPushButton:disabled {
        background-color: #dcdcdc;
        color: #888888;
    }
    QStatusBar {
        font-size: 12px;
    }
    QProgressBar {
        border: 1px solid #adadad;
        border-radius: 5px;
        text-align: center;
        background-color: #ffffff;
    }
    QProgressBar::chunk {
        background-color: #007acc;
        border-radius: 4px;
    }
    QCheckBox::indicator {
        width: 18px;
        height: 18px;
    }
    QCheckBox::indicator:unchecked {
        background-color: #ffffff;
        border: 1px solid #adadad;
        border-radius: 4px;
    }
    QCheckBox::indicator:checked {
        background-color: #007acc;
        border: 1px solid #007acc;
        border-radius: 4px;
    }
"""

class FolderWorker(QtCore.QObject):
    """
    Worker thread to process files without freezing the GUI.
    """
    finished = QtCore.Signal()
    text_ready = QtCore.Signal(str)
    progress = QtCore.Signal(int)
    
    def __init__(self, folder_path):
        super().__init__()
        self.folder_path = folder_path

    def run(self):
        """Long-running task."""
        all_text_parts = []
        
        # First pass: count total files for progress bar
        total_files = sum(len(files) for _, _, files in os.walk(self.folder_path))
        files_processed = 0

        for root, _, files in os.walk(self.folder_path):
            for filename in files:
                file_path = os.path.join(root, filename)
                separator = f"\n\n{'='*20}\n--- File: {file_path} ---\n{'='*20}\n\n"
                all_text_parts.append(separator)
                
                try:
                    with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                        all_text_parts.append(f.read())
                except Exception as e:
                    all_text_parts.append(f"Could not read file. Error: {e}")

                files_processed += 1
                if total_files > 0:
                    percentage = int((files_processed / total_files) * 100)
                    self.progress.emit(percentage)
        
        final_text = "".join(all_text_parts)
        self.text_ready.emit(final_text)
        self.finished.emit()

class MainWindow(QtWidgets.QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("Awesome Text Extractor")
        self.setWindowIcon(self.style().standardIcon(QtWidgets.QStyle.StandardPixmap.SP_FileIcon))
        self.setGeometry(100, 100, 900, 700)
        self.setAcceptDrops(True) # Enable Drag and Drop
        self.init_ui()
        
    def init_ui(self):
        # --- Central Widget & Layout ---
        central_widget = QtWidgets.QWidget()
        self.setCentralWidget(central_widget)
        self.layout = QtWidgets.QVBoxLayout(central_widget)

        # --- Text Area ---
        self.text_area = QtWidgets.QTextEdit()
        self.text_area.setReadOnly(True)
        self.text_area.setPlaceholderText("Drag a folder here or use the 'Select Folder' button to begin.")
        self.layout.addWidget(self.text_area)

        # --- Button Layout ---
        button_layout = QtWidgets.QHBoxLayout()
        
        self.select_folder_btn = QtWidgets.QPushButton(" Select Folder")
        self.select_folder_btn.setIcon(self.style().standardIcon(QtWidgets.QStyle.StandardPixmap.SP_DirOpenIcon))
        self.select_folder_btn.clicked.connect(self.select_folder)
        button_layout.addWidget(self.select_folder_btn)
        
        self.copy_btn = QtWidgets.QPushButton(" Copy to Clipboard")
        self.copy_btn.setIcon(self.style().standardIcon(QtWidgets.QStyle.StandardPixmap.SP_FileDialogContentsView))
        self.copy_btn.clicked.connect(self.copy_to_clipboard)
        self.copy_btn.setEnabled(False) # Disabled until there is text
        button_layout.addWidget(self.copy_btn)

        button_layout.addStretch() # Pushes the checkbox to the right

        self.theme_toggle = QtWidgets.QCheckBox("Dark Mode")
        self.theme_toggle.setChecked(True)
        self.theme_toggle.stateChanged.connect(self.toggle_theme)
        button_layout.addWidget(self.theme_toggle)

        self.layout.addLayout(button_layout)
        
        # --- Status Bar and Progress Bar ---
        self.status_bar = QtWidgets.QStatusBar()
        self.setStatusBar(self.status_bar)
        self.progress_bar = QtWidgets.QProgressBar()
        self.progress_bar.setVisible(False)
        self.status_bar.addPermanentWidget(self.progress_bar)
        self.status_bar.showMessage("Ready. Drag a folder or click 'Select Folder'.")

        self.toggle_theme() # Apply initial theme

    def select_folder(self):
        folder_path = QtWidgets.QFileDialog.getExistingDirectory(self, "Select Folder")
        if folder_path:
            self.start_processing(folder_path)

    def start_processing(self, folder_path):
        self.text_area.clear()
        self.copy_btn.setEnabled(False)
        self.select_folder_btn.setEnabled(False)
        self.status_bar.showMessage(f"Processing folder: {folder_path}...")
        self.progress_bar.setValue(0)
        self.progress_bar.setVisible(True)

        # Setup worker thread
        self.thread = QtCore.QThread()
        self.worker = FolderWorker(folder_path)
        self.worker.moveToThread(self.thread)
        
        # Connect signals and slots
        self.thread.started.connect(self.worker.run)
        self.worker.finished.connect(self.thread.quit)
        self.worker.finished.connect(self.worker.deleteLater)
        self.thread.finished.connect(self.thread.deleteLater)
        self.worker.text_ready.connect(self.on_text_ready)
        self.worker.progress.connect(self.update_progress)
        self.thread.finished.connect(self.on_processing_finished)

        self.thread.start()

    def on_text_ready(self, text):
        self.text_area.setPlainText(text)
        self.copy_btn.setEnabled(True)

    def update_progress(self, value):
        self.progress_bar.setValue(value)

    def on_processing_finished(self):
        self.status_bar.showMessage("Processing complete.", 5000) # Message for 5 seconds
        self.progress_bar.setVisible(False)
        self.select_folder_btn.setEnabled(True)

    def copy_to_clipboard(self):
        clipboard = QtWidgets.QApplication.clipboard()
        clipboard.setText(self.text_area.toPlainText())
        self.status_bar.showMessage("Text copied to clipboard!", 3000)

    def toggle_theme(self):
        if self.theme_toggle.isChecked():
            self.setStyleSheet(DARK_STYLE)
            self.setWindowIcon(self.style().standardIcon(QtWidgets.QStyle.StandardPixmap.SP_FileIcon))
        else:
            self.setStyleSheet(LIGHT_STYLE)
            # Re-apply icons as they can be affected by stylesheet changes
            self.setWindowIcon(self.style().standardIcon(QtWidgets.QStyle.StandardPixmap.SP_FileIcon))
        self.select_folder_btn.setIcon(self.style().standardIcon(QtWidgets.QStyle.StandardPixmap.SP_DirOpenIcon))
        self.copy_btn.setIcon(self.style().standardIcon(QtWidgets.QStyle.StandardPixmap.SP_FileDialogContentsView))


    # --- Drag and Drop Events ---
    def dragEnterEvent(self, event: QtGui.QDragEnterEvent):
        # Accept the drop if it contains a URL for a local folder
        if event.mimeData().hasUrls():
            url = event.mimeData().urls()[0]
            if url.isLocalFile() and os.path.isdir(url.toLocalFile()):
                event.acceptProposedAction()

    def dropEvent(self, event: QtGui.QDropEvent):
        url = event.mimeData().urls()[0]
        folder_path = url.toLocalFile()
        self.start_processing(folder_path)

if __name__ == "__main__":
    app = QtWidgets.QApplication(sys.argv)
    window = MainWindow()
    
    # Check if a folder was dragged onto the script file itself
    if len(sys.argv) > 1 and os.path.isdir(sys.argv[1]):
        # Post the processing task to the event loop to run after window is shown
        QtCore.QTimer.singleShot(100, lambda: window.start_processing(sys.argv[1]))

    window.show()
    sys.exit(app.exec())