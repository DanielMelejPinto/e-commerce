import os
import re

def sort_imports(content):
    lines = content.split('\n')
    out = []
    imports = []
    in_imports = False
    
    for i, line in enumerate(lines):
        if line.startswith('import '):
            imports.append(line)
        else:
            if imports and line.strip() == '':
                continue
            if imports and not line.startswith('import '):
                # We finished collecting imports. Sort and emit them.
                def import_key(imp):
                    is_static = imp.startswith('import static ')
                    rest = imp.replace('import static ', '').replace('import ', '').strip()
                    
                    if rest.startswith('java.'): group = 1
                    elif rest.startswith('jakarta.') or rest.startswith('javax.'): group = 2
                    elif rest.startswith('org.'): group = 3
                    elif rest.startswith('io.') or rest.startswith('net.') or rest.startswith('com.'): group = 4
                    else: group = 5
                    
                    return (0 if is_static else 1, group, rest)
                
                imports.sort(key=import_key)
                
                # Emit them with groups separated by newlines
                last_is_static = None
                last_group = None
                
                for imp in imports:
                    is_static = imp.startswith('import static ')
                    rest = imp.replace('import static ', '').replace('import ', '').strip()
                    
                    if rest.startswith('java.'): group = 1
                    elif rest.startswith('jakarta.') or rest.startswith('javax.'): group = 2
                    elif rest.startswith('org.'): group = 3
                    elif rest.startswith('io.') or rest.startswith('net.') or rest.startswith('com.'): group = 4
                    else: group = 5
                    
                    if last_group is not None:
                        if is_static != last_is_static or group != last_group:
                            out.append('')
                            
                    out.append(imp)
                    last_is_static = is_static
                    last_group = group
                
                out.append('')
                out.append(line)
                imports = []
            else:
                out.append(line)
                
    return '\n'.join(out)

for root, _, files in os.walk('.'):
    for file in files:
        if file.endswith('.java'):
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()
            new_content = sort_imports(content)
            # Remove consecutive blank lines
            new_content = re.sub(r'\n{3,}', '\n\n', new_content)
            if new_content != content:
                with open(path, 'w') as f:
                    f.write(new_content)
