---
name: react-patterns
description: Comprehensive React patterns covering state management (Zustand, Redux Toolkit, Jotai, React Query), performance optimization (40+ rules), and modernization (hooks migration, concurrent features). The single source of truth for all React development in PayU.
dependencies: [nextjs-app-router-patterns, frontend-patterns]
---

# React Patterns - PayU Complete Guide

This skill consolidates all React patterns for the PayU Digital Banking Platform: state management, performance optimization, and modernization patterns.

## When to Use This Skill

- Building or refactoring React/Next.js applications
- Choosing and implementing state management solutions
- Optimizing React application performance
- Migrating from class components to hooks
- Implementing concurrent React features

---

## Part 1: State Management

### State Categories

| Type | Description | Solutions |
|------|-------------|-----------|
| **Local State** | Component-specific, UI state | `useState`, `useReducer` |
| **Global State** | Shared across components | Zustand, Redux Toolkit, Jotai |
| **Server State** | Remote data, caching | React Query, SWR |
| **URL State** | Route parameters, search | React Router, nuqs |
| **Form State** | Input values, validation | React Hook Form |

### Selection Criteria

```
Small app, simple state → Zustand or Jotai
Large app, complex state → Redux Toolkit
Heavy server interaction → React Query + light client state
Atomic/granular updates → Jotai
```

### Pattern 1: Zustand (Recommended for PayU)

```typescript
// store/useStore.ts
import { create } from 'zustand'
import { devtools, persist } from 'zustand/middleware'

interface AppState {
  user: User | null
  theme: 'light' | 'dark'
  setUser: (user: User | null) => void
  toggleTheme: () => void
}

export const useStore = create<AppState>()(
  devtools(
    persist(
      (set) => ({
        user: null,
        theme: 'light',
        setUser: (user) => set({ user }),
        toggleTheme: () => set((state) => ({
          theme: state.theme === 'light' ? 'dark' : 'light'
        })),
      }),
      { name: 'app-storage' }
    )
  )
)

// Usage in component
function Header() {
  const { user, theme, toggleTheme } = useStore()
  return (
    <header className={theme}>
      {user?.name}
      <button onClick={toggleTheme}>Toggle Theme</button>
    </header>
  )
}
```

### Pattern 2: Redux Toolkit (Complex Applications)

```typescript
// store/index.ts
import { configureStore } from "@reduxjs/toolkit";
import { TypedUseSelectorHook, useDispatch, useSelector } from "react-redux";
import userReducer from "./slices/userSlice";

export const store = configureStore({
  reducer: { user: userReducer },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
export const useAppDispatch: () => AppDispatch = useDispatch;
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;

// store/slices/userSlice.ts
import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";

export const fetchUser = createAsyncThunk(
  "user/fetchUser",
  async (userId: string) => {
    const response = await fetch(`/api/users/${userId}`);
    return response.json();
  },
);

const userSlice = createSlice({
  name: "user",
  initialState: { current: null, status: "idle" },
  reducers: {
    setUser: (state, action) => { state.current = action.payload; },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchUser.pending, (state) => { state.status = "loading"; })
      .addCase(fetchUser.fulfilled, (state, action) => {
        state.status = "succeeded";
        state.current = action.payload;
      });
  },
});
```

### Pattern 3: React Query for Server State

```typescript
// hooks/useUsers.ts
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";

export const userKeys = {
  all: ["users"] as const,
  detail: (id: string) => [...userKeys.all, "detail", id] as const,
};

export function useUser(id: string) {
  return useQuery({
    queryKey: userKeys.detail(id),
    queryFn: () => fetchUser(id),
    enabled: !!id,
    staleTime: 5 * 60 * 1000,
  });
}

// Mutation with optimistic update
export function useUpdateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: updateUser,
    onMutate: async (newUser) => {
      await queryClient.cancelQueries({ queryKey: userKeys.detail(newUser.id) });
      const previousUser = queryClient.getQueryData(userKeys.detail(newUser.id));
      queryClient.setQueryData(userKeys.detail(newUser.id), newUser);
      return { previousUser };
    },
    onError: (err, newUser, context) => {
      queryClient.setQueryData(userKeys.detail(newUser.id), context?.previousUser);
    },
  });
}
```

---

## Part 2: Performance Optimization

### Critical Priorities

1. **Eliminate Waterfalls** - Use `Promise.all()` for independent operations
2. **Avoid Barrel Imports** - Import directly from source files
3. **Dynamic Imports** - Lazy-load heavy components
4. **Strategic Suspense** - Stream content progressively

### Performance Patterns

```typescript
// ❌ Waterfall - Sequential awaits
const user = await fetchUser()
const posts = await fetchPosts(user.id) // Depends on user
const comments = await fetchComments() // Independent but waits

// ✅ Parallel - Independent operations
const [user, comments] = await Promise.all([
  fetchUser(),
  fetchComments()
])
const posts = await fetchPosts(user.id) // Depends on user

// ❌ Barrel import
import { Check, X, Plus } from 'lucide-react'

// ✅ Direct import
import Check from 'lucide-react/dist/esm/icons/check'
import X from 'lucide-react/dist/esm/icons/x'

// Dynamic import for heavy components
import dynamic from 'next/dynamic'
const MonacoEditor = dynamic(() => import('./monaco-editor'), { ssr: false })
```

### Re-render Optimization

```typescript
// ✅ Memoize expensive components
const ExpensiveList = React.memo(function ExpensiveList({ items }) {
  return items.map(item => <Item key={item.id} {...item} />)
})

// ✅ Use useMemo for expensive computations
const sortedItems = useMemo(() => 
  items.sort((a, b) => b.score - a.score),
  [items]
)

// ✅ Use useCallback for stable callbacks
const handleSubmit = useCallback((data: FormData) => {
  submitForm(data)
}, []) // Stable reference
```

---

## Part 3: Modernization Patterns

### Class to Hooks Migration

```typescript
// Before: Class component
class Counter extends React.Component {
  state = { count: 0 };
  increment = () => this.setState({ count: this.state.count + 1 });
  render() {
    return <button onClick={this.increment}>{this.state.count}</button>;
  }
}

// After: Functional component with hooks
function Counter() {
  const [count, setCount] = useState(0);
  const increment = () => setCount(c => c + 1);
  return <button onClick={increment}>{count}</button>;
}
```

### Lifecycle to useEffect

```typescript
// Before: Lifecycle methods
componentDidMount() { this.fetchData(); }
componentDidUpdate(prevProps) {
  if (prevProps.id !== this.props.id) this.fetchData();
}
componentWillUnmount() { this.cancelRequest(); }

// After: useEffect
useEffect(() => {
  let cancelled = false;
  fetchData();
  return () => { cancelled = true; };
}, [id]); // Re-run when id changes
```

### Concurrent Features (React 18+)

```typescript
// useTransition for non-urgent updates
const [isPending, startTransition] = useTransition();

function handleFilterChange(newFilter) {
  // Urgent: Update input
  setInputValue(newFilter);
  
  // Non-urgent: Update filtered list
  startTransition(() => {
    setFilter(newFilter);
  });
}

// useDeferredValue for deferring re-renders
const deferredQuery = useDeferredValue(query);

// Suspense for data fetching
<Suspense fallback={<Skeleton />}>
  <UserProfile userId={id} />
</Suspense>
```

---

## Best Practices Summary

### Do's
- **Colocate state** - Keep state as close to usage as possible
- **Use selectors** - Prevent unnecessary re-renders
- **Separate concerns** - Server state (React Query) vs client state (Zustand)
- **Profile first** - Use React DevTools before optimizing
- **Use TypeScript** - Full type coverage prevents errors

### Don'ts
- **Don't over-globalize** - Not everything needs global state
- **Don't duplicate server state** - Let React Query manage it
- **Don't block with waterfalls** - Parallelize independent operations
- **Don't use barrel imports** - Import directly from source
- **Don't mutate state** - Always use immutable updates

---

## Resources

- [React Documentation](https://react.dev)
- [Redux Toolkit](https://redux-toolkit.js.org/)
- [Zustand](https://github.com/pmndrs/zustand)
- [TanStack Query](https://tanstack.com/query)
- [Next.js Performance](https://nextjs.org/docs/app/building-your-application/optimizing)
