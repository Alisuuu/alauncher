import os
import shutil
import zipfile
import re
from pathlib import Path

def get_backups(target_dir):
    if not target_dir.exists():
        return []
    pattern = re.compile(r"^(\d+)\.zip$")
    backups = []
    for f in sorted(target_dir.iterdir()):
        match = pattern.match(f.name)
        if match:
            backups.append((int(match.group(1)), f))
    backups.sort(key=lambda x: x[0])
    return backups

def main():
    current_dir = Path(os.getcwd())
    if current_dir.name == "app":
        project_root = current_dir.parent
    else:
        project_root = current_dir

    backup_dir = project_root.parent / "Backup"

    backups = get_backups(backup_dir)
    if not backups:
        print("[!] Nenhum backup encontrado em:", backup_dir)
        return

    print("[*] Backups disponiveis:")
    for num, path in backups:
        size_mb = path.stat().st_size / (1024 * 1024)
        print(f"    {num}.zip  ({size_mb:.1f} MB)")

    choice = input("\nDigite o numero do backup para restaurar (ou 'c' para cancelar): ").strip()
    if choice.lower() == "c":
        print("[*] Cancelado.")
        return

    if not choice.isdigit():
        print("[!] Entrada invalida.")
        return

    zip_path = backup_dir / f"{choice}.zip"
    if not zip_path.exists():
        print(f"[!] Backup {choice}.zip nao encontrado.")
        return

    # Arquivos/pastas que o pack.py salva no zip (exclui __pycache__, .git, build folders exceto APKs)
    # Vamos excluir tudo que esta dentro de project_root e extrair o zip por cima
    print(f"[*] Restaurando backup {choice}.zip...")
    print(f"[*] Limpando projeto atual...")

    # Limpa tudo dentro de project_root (exceto .gradle e .acside que ja sao limpos pelo pack.py)
    for item in project_root.iterdir():
        if item.name in (".gradle", ".acside"):
            continue
        if item.name == "Backup":
            continue
        try:
            if item.is_dir():
                shutil.rmtree(item)
            else:
                item.unlink()
        except Exception as e:
            print(f"[!] Erro ao remover {item}: {e}")

    print(f"[*] Extraindo {choice}.zip...")
    with zipfile.ZipFile(zip_path, 'r') as zipf:
        zipf.extractall(project_root)

    print(f"[+] Projeto restaurado com sucesso a partir de {choice}.zip!")

if __name__ == "__main__":
    main()
