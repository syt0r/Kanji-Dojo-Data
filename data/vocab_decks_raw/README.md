JLPT sets from tanos from js console:

```
$$('tbody>tr').map(x=>Array.from(x.querySelectorAll("a")).map(x1=>x1.textContent).join(",")).join("\n")
```