// Every h2 folds the content up to the next h2. A folded heading dims so open sections stand out.
document.querySelectorAll("h2").forEach((heading) => {
  heading.tabIndex = 0;
  heading.setAttribute("role", "button");
  heading.setAttribute("aria-expanded", "true");
  const toggle = () => {
    const closed = heading.classList.toggle("closed");
    heading.setAttribute("aria-expanded", String(!closed));
    for (let el = heading.nextElementSibling; el && el.tagName !== "H2" && !el.querySelector("h2"); el = el.nextElementSibling) {
      el.hidden = closed;
    }
  };
  heading.addEventListener("click", toggle);
  heading.addEventListener("keydown", (event) => {
    if (event.key === "Enter" || event.key === " ") { event.preventDefault(); toggle(); }
  });
});
