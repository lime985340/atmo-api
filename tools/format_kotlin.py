#!/usr/bin/env python3
import re
from pathlib import Path


def format_kotlin(text: str) -> str:
    # Normalize line endings
    text = text.replace('\r\n', '\n').replace('\r', '\n')

    # Replace tabs with 4 spaces
    text = text.replace('\t', ' ' * 4)

    # Trim trailing whitespace on each line
    lines = [re.sub(r"[ \t]+$", '', l) for l in text.split('\n')]

    # Ensure single blank line after package declaration
    out_lines = []
    i = 0
    n = len(lines)
    while i < n:
        out_lines.append(lines[i])
        if lines[i].startswith('package '):
            # skip following blank lines, leave exactly one
            j = i + 1
            while j < n and lines[j].strip() == '':
                j += 1
            # ensure one blank line if next line is not imports
            if j < n and not lines[j].startswith('import '):
                out_lines.append('')
            i = j
            continue
        i += 1

    text = '\n'.join(out_lines)

    # Collapse multiple consecutive blank lines to max 2
    text = re.sub(r"\n{3,}", '\n\n', text)

    # Ensure a blank line between import block and rest
    text = re.sub(r"(import .+(?:\nimport .+)*)\n(?!import )", lambda m: m.group(1) + '\n\n', text)

    # Space after commas
    text = re.sub(r",([^ \n])", r", \1", text)

    # Space around = in assignments (but not in function params with default values in certain contexts)
    text = re.sub(r"([a-zA-Z_)\]])\s*=\s*([a-zA-Z_(\"\'\[])", r"\1 = \2", text)

    # Space around : for types
    text = re.sub(r"([\w)\]])\s*:\s*([A-Za-z0-9_<\[\(\?])", r"\1: \2", text)

    # Space after keywords (fun, val, var, if, when, for, while, etc.)
    keywords = r'\b(fun|val|var|if|else|when|for|while|return|import|package)\b'
    text = re.sub(f'({keywords})\\s*\\(', r'\1 (', text)

    # Remove extra spaces in lines with assignments - normalize to single space
    text = re.sub(r'(\w+)\s{2,}=', r'\1 =', text)

    # Ensure final newline
    if not text.endswith('\n'):
        text += '\n'

    return text


def main():
    root = Path(__file__).resolve().parents[1]
    kt_files = list(root.rglob('*.kt')) + list(root.rglob('*.kts'))
    changed = []
    for p in kt_files:
        try:
            orig = p.read_text(encoding='utf-8')
        except Exception:
            continue
        new = format_kotlin(orig)
        if new != orig:
            p.write_text(new, encoding='utf-8')
            changed.append(str(p.relative_to(root)))

    if changed:
        print('Formatted files:')
        for c in changed:
            print(' -', c)
    else:
        print('No changes')


if __name__ == '__main__':
    main()
