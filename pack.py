import os
import shutil
import zipfile
import re
from pathlib import Path

def get_next_zip_name(target_dir):
    if not target_dir.exists():
        target_dir.mkdir(parents=True, exist_ok=True)
        return "1.zip"
    
    files = os.listdir(target_dir)
    pattern = re.compile(rf"^(\d+)\.zip$")
    numbers = []
    for f in files:
        match = pattern.match(f)
        if match:
            numbers.append(int(match.group(1)))
    
    next_num = max(numbers) + 1 if numbers else 1
    return f"{next_num}.zip"

def main():
    # Detect where we are
    current_dir = Path(os.getcwd())
    
    # Root of the workspace is 'ambiente'
    # The script is located inside 'ambiente' (but called from its subfolders)
    # The target 'alicrg' is a SIBLING of 'ambiente'
    
    if current_dir.name == "app":
        project_root = current_dir.parent
    else:
        project_root = current_dir

    # Target is sibling of project_root
    target_dir = project_root.parent / "Backup"
    
    print(f"[*] Project root: {project_root}")
    print(f"[*] Target directory: {target_dir}")

    # 0. Ask for changes
    change_description = input("O que mudou neste backup? (Deixe vazio para ignorar): ").strip()
    changes_file = project_root / "changes.txt"
    has_changes_file = False
    
    if change_description:
        with open(changes_file, "w", encoding="utf-8") as f:
            f.write(change_description)
        has_changes_file = True

    # 1. Cleanup
    dirs_to_clean = [
        project_root / "app" / ".gradle",
        project_root / "app" / ".cxx",
        project_root / "app" / ".acside",
    ]
    
    print("[*] Performing cleanup (ignoring build folders)...")
    for d in dirs_to_clean:
        if d.exists() and d.is_dir():
            try:
                shutil.rmtree(d)
                print(f"[*] Cleaned: {d.name}")
            except: pass

    # 2. Versioning
    zip_name = get_next_zip_name(target_dir)
    zip_path = target_dir / zip_name
    print(f"[*] Target ZIP: {zip_path}")

    # 3. Zipping
    print(f"[*] Creating {zip_name}...")
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
        # Walk through project_root
        for file in project_root.rglob('*'):
            if "__pycache__" in str(file) or ".git" in str(file):
                continue
                
            # Special rule for build: only APKs in actual build/ directories
            path_parts = file.relative_to(project_root).parts
            if "build" in path_parts:
                if file.is_file() and file.name.endswith(".apk"):
                    zipf.write(file, file.relative_to(project_root))
                continue
            
            if file.is_file():
                # Avoid zipping the zip itself if it was somehow inside
                if str(zip_path) == str(file):
                    continue
                zipf.write(file, file.relative_to(project_root))

    if has_changes_file and changes_file.exists():
        changes_file.unlink()

    print(f"[+] Success! Backup created at {zip_path}")

if __name__ == "__main__":
    main()
